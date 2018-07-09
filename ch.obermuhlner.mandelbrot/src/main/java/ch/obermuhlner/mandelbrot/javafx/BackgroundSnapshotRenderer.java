package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.imageio.ImageIO;

import ch.obermuhlner.mandelbrot.math.BigDecimalMath;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.render.AutoPrecisionMandelbrotRenderer;
import ch.obermuhlner.mandelbrot.render.MandelbrotRenderer;
import ch.obermuhlner.mandelbrot.util.StopWatch;
import ch.obermuhlner.mandelbrot.util.ThreadInterruptedException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;

public class BackgroundSnapshotRenderer extends Thread {

	private static final BigDecimal TWO = new BigDecimal(2);
	
	private Deque<SnapshotRequest> pendingSnapshotRequests = new ArrayDeque<>();
	private volatile boolean running;

	private int pendingCount = 0;
	
	private ObservableList<SnapshotRequest> snapshotRequests = FXCollections.observableArrayList();
	
	private final MandelbrotRenderer mandelbrotRenderer = new AutoPrecisionMandelbrotRenderer();

	public synchronized int getPendingSnapshotRequestCount() {
		return pendingCount;
	}
	
	public synchronized void stopRunning() {
		running = false;
		notifyAll();
	}
	
	public synchronized void cancelAllSnapshotRequestsAndStopRunning() {
		pendingSnapshotRequests.clear();
		snapshotRequests.clear();
		//stopRunning();
		
		interrupt();
	}

	public synchronized void addSnapshotRequest(SnapshotRequest snapshotRequest) {
		snapshotRequests.add(snapshotRequest);
		pendingSnapshotRequests.add(snapshotRequest);
		pendingCount++;
		notifyAll();
	}

	@Override
	public void run() {
		try {
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
					synchronized(this) {
						pendingCount--;
					}
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
		} catch (ThreadInterruptedException ex) {
			// ignore end leave thread
		}
	}

	private void draw(SnapshotRequest snapshotRequest) {
		StopWatch stopWatch = new StopWatch();
		snapshotRequest.snapshotStatusProperty().set(SnapshotStatus.Calculating);
		renderImage(
				snapshotRequest.file,
				snapshotRequest.drawRequest.x,
				snapshotRequest.drawRequest.y,
				TWO,
				BigDecimal.valueOf(snapshotRequest.drawRequest.zoom),
				snapshotRequest.drawRequest.maxIteration,
				snapshotRequest.palette,
				snapshotRequest.width,
				snapshotRequest.height,
				new UiThreadProgress(snapshotRequest));
		snapshotRequest.snapshotStatusProperty().set(SnapshotStatus.Done);
		snapshotRequest.setCalculationMillis((long) stopWatch.getElapsedMilliseconds());
	}

	private void renderImage(File file, BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomPower, int maxIterations, Palette palette, int imageWidth, int imageHeight, Progress progress) {
		if (file.exists()) {
			System.out.println("Already calculated " + file.getName() + " with zoom " + zoomPower.toPlainString());
			return;
		}

		int precision = zoomPower.intValue() * 1 + 10;
		MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
		BigDecimal radius = zoomStart.multiply(BigDecimalMath.tenToThePowerOf(zoomPower.negate(), mc));
		BigDecimal minWidthHeight = new BigDecimal(Math.min(imageWidth, imageHeight));
		BigDecimal xRadius = radius.multiply(new BigDecimal(imageWidth), mc).divide(minWidthHeight, mc);
		BigDecimal yRadius = radius.multiply(new BigDecimal(imageHeight), mc).divide(minWidthHeight, mc);

		WritableImageMandelbrotResult result = new WritableImageMandelbrotResult(imageWidth, imageHeight, palette);
		mandelbrotRenderer.drawMandelbrot(result, xCenter, yCenter, xRadius, yRadius, precision, maxIterations, imageWidth, imageHeight, progress);
		
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(result.getImage(), null), "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ObservableList<SnapshotRequest> getSnapshotRequests() {
		return snapshotRequests;
	}

	public void removeSnapshotRequest(SnapshotRequest snapshotRequest) {
		pendingSnapshotRequests.remove(snapshotRequest);
		snapshotRequests.remove(snapshotRequest);
	}	
}
