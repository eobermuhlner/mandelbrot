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
import ch.obermuhlner.mandelbrot.render.AutoPrecisionMandelbrotRenderer;
import ch.obermuhlner.mandelbrot.render.MandelbrotRenderer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

public class BackgroundSnapshotRenderer extends Thread {

	private static final BigDecimal TWO = new BigDecimal(2);
	
	private Deque<SnapshotRequest> pendingSnapshotRequests = new ArrayDeque<>();
	private volatile boolean running;

	private ObservableList<SnapshotRequest> snapshotRequests = FXCollections.observableArrayList();
	
	private final MandelbrotRenderer mandelbrotRenderer = new AutoPrecisionMandelbrotRenderer();

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
			
			if (running) {
				try {
					synchronized(this) {
						wait();
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
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
				new UiThreadProgress(snapshotRequest));
	}

	private void renderImage(File file, BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomPower, Palette palette, Progress progress) {
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

		WritableImage image = mandelbrotRenderer.drawMandelbrot(xCenter, yCenter, radius, radius, precision, maxIterations, imageWidth, imageHeight, palette, progress);
		
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ObservableList<SnapshotRequest> getSnapshotRequests() {
		return snapshotRequests;
	}	
}
