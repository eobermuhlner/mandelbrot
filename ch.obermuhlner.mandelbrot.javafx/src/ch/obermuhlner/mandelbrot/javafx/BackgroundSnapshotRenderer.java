package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.imageio.ImageIO;

import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import ch.obermuhlner.mandelbrot.math.BigDecimalMath;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class BackgroundSnapshotRenderer extends Thread {

	private static final BigDecimal TWO = new BigDecimal(2);
	private static final BigDecimal TWO_SQUARE = new BigDecimal(2*2);

	private static final BigDecimal DOUBLE_THRESHOLD = new BigDecimal("0.00000000002");

	private Deque<SnapshotRequest> pendingSnapshotRequests = new ArrayDeque<>();
	private volatile boolean running;

	private ObservableList<SnapshotRequest> snapshotRequests = FXCollections.observableArrayList(); 

	public synchronized int getPendingSnapshotRequestCount() {
		return pendingSnapshotRequests.size();
	}
	
	public synchronized void stopRunning() {
		running = false;
		notifyAll();
	}

	public synchronized void addSnapshotRequest(SnapshotRequest snapshotRequest) {
		snapshotRequests.add(snapshotRequest);
		pendingSnapshotRequests.add(snapshotRequest);
		notifyAll();
	}

	@Override
	public void run() {
		running = true;
		
		while (running) {
			SnapshotRequest snapshotRequest = null;
			
			synchronized(this) {
				if (!pendingSnapshotRequests.isEmpty()) {
					snapshotRequest = pendingSnapshotRequests.pop();
				}
			}
			
			if (snapshotRequest != null) {
				draw(snapshotRequest);
			}
			
			synchronized(this) {
				if (running && pendingSnapshotRequests.isEmpty()) {
					try {
						wait();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	private void draw(SnapshotRequest snapshotRequest) {
		renderImage(
				snapshotRequest.file,
				snapshotRequest.drawRequest.x,
				snapshotRequest.drawRequest.y,
				TWO,
				BigDecimal.valueOf(snapshotRequest.drawRequest.zoom),
				snapshotRequest.palette,
				snapshotRequest);
	}

	private static void renderImage(File file, BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomPower, Palette palette, Progress progress) {
		if (file.exists()) {
			System.out.println("Already calculated " + file.getName() + " with zoom " + zoomPower.toPlainString());
			return;
		}

		int precision = zoomPower.intValue() * 1 + 10;
		MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
		BigDecimal radius = zoomStart.multiply(BigDecimalMath.tenToThePowerOf(zoomPower.negate(), mc));
		int maxIterations = 1000 + zoomPower.intValue() * 100;
		int imageWidth = 800;
		int imageHeight = 800;

		WritableImage image = drawMandelbrot(
				xCenter,
				yCenter,
				radius,
				radius,
				precision,
				maxIterations,
				imageWidth,
				imageHeight,
				palette,
				progress);
		
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static WritableImage drawMandelbrot(BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette, Progress progress) {
		if (xRadius.compareTo(DOUBLE_THRESHOLD) > 0 && yRadius.compareTo(DOUBLE_THRESHOLD) > 0) {
			return drawMandelbrotDouble(xCenter.doubleValue(), yCenter.doubleValue(), xRadius.doubleValue(), yRadius.doubleValue(), maxIterations, imageWidth, imageHeight, palette, progress);
		} else {
			return drawMandelbrotBigDecimal(xCenter, yCenter, xRadius, yRadius, precision, maxIterations, imageWidth, imageHeight, palette, progress);
		}
	}
	
	private static WritableImage drawMandelbrotDouble(double xCenter, double yCenter, double xRadius, double yRadius, int maxIterations, int imageWidth, int imageHeight, Palette palette, Progress progress) {
		WritableImage image = new WritableImage(imageWidth, imageHeight);
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double stepX = xRadius*2 / imageWidth;
		double stepY = yRadius*2 / imageHeight;
		double x0 = 0 - xCenter - xRadius; 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			double y0 = 0 - yCenter - yRadius; 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
				double x = 0;
				double y = 0;
				int iterations = 0;
				double xx = x*x;
				double yy = y*y;
				while (xx + yy < 2*2 && iterations < maxIterations) {
					y = 2*x*y + y0;
					x = xx - yy + x0;
					iterations++;
					
					xx = x*x;
					yy = y*y;
				}

				Color color = iterations == maxIterations ? Color.BLACK : palette.getColor(iterations);
				pixelWriter.setColor(pixelX, pixelY, color);

				y0 += stepY;
			}
			x0 += stepX;
			
			double progressValue = (double)pixelX / imageWidth;
			Platform.runLater(() -> {
				progress.setProgress(progressValue);
			});
		}
		
		Platform.runLater(() -> {
			progress.setProgress(1.0);
		});

		return image;
	}	
	
	private static WritableImage drawMandelbrotBigDecimal(BigDecimal xCenterBigDecimal, BigDecimal yCenterBigDecimal, BigDecimal xRadiusBigDecimal, BigDecimal yRadiusBigDecimal, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette, Progress progress) {
		WritableImage image = new WritableImage(imageWidth, imageHeight);
		PixelWriter pixelWriter = image.getPixelWriter();
		
		MathContext mc = new MathContext(precision, RoundingMode.HALF_EVEN);
		
		BigDecimal xCenter = new BigDecimal(xCenterBigDecimal.toString());
		BigDecimal yCenter = new BigDecimal(yCenterBigDecimal.toString());
		BigDecimal xRadius = new BigDecimal(xRadiusBigDecimal.toString());
		BigDecimal yRadius = new BigDecimal(xRadiusBigDecimal.toString());

		BigDecimal stepX = xRadius.multiply(TWO, mc).divide(BigDecimal.valueOf(imageWidth), mc);
		BigDecimal stepY = yRadius.multiply(TWO, mc).divide(BigDecimal.valueOf(imageHeight), mc);
		BigDecimal x0 = xCenter.negate().subtract(xRadius, mc); 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			BigDecimal y0 = yCenter.negate().subtract(yRadius, mc); 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
				BigDecimal x = BigDecimal.ZERO;
				BigDecimal y = BigDecimal.ZERO;
				int iterations = 0;
				BigDecimal xx = x.multiply(x, mc);
				BigDecimal yy = y.multiply(y, mc);
				while (xx.add(yy, mc).compareTo(TWO_SQUARE) < 0 && iterations < maxIterations) {
					y = TWO.multiply(x, mc).multiply(y, mc).add(y0, mc);
					x = xx.subtract(yy, mc).add(x0, mc);
					iterations++;
					
					xx = x.multiply(x, mc);
					yy = y.multiply(y, mc);
				}

				Color color = iterations == maxIterations ? Color.BLACK : palette.getColor(iterations);
				pixelWriter.setColor(pixelX, pixelY, color);

				y0 = y0.add(stepY);
			}
			x0 = x0.add(stepX);

			double progressValue = (double)pixelX / imageWidth;
			Platform.runLater(() -> {
				progress.setProgress(progressValue);
			});
		}

		Platform.runLater(() -> {
			progress.setProgress(1.0);
		});
		
		return image;
	}

	public ObservableList<SnapshotRequest> getSnapshotRequests() {
		return snapshotRequests;
	}	
}
