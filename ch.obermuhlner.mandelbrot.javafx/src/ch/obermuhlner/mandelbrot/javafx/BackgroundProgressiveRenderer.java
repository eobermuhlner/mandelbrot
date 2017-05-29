package ch.obermuhlner.mandelbrot.javafx;

import javafx.application.Platform;

public class BackgroundProgressiveRenderer extends Thread {
	private final MandelbrotApp mandelbrotApp;
	
	private volatile boolean running;
	private DrawRequest nextDrawRequest;
	
	public BackgroundProgressiveRenderer(MandelbrotApp mandelbrotApp) {
		this.mandelbrotApp = mandelbrotApp;
	}
	
	synchronized  void triggerDraw(DrawRequest drawRequest) {
		nextDrawRequest = drawRequest;
		notifyAll();
	}
	
	private synchronized DrawRequest getNextDrawRequest() {
		DrawRequest result = nextDrawRequest;
		nextDrawRequest = null;
		return result;
	}
	
	public synchronized void stopRunning() {
		running = false;
		notifyAll();
	}

	public void run() {
		running = true;

		while (running) {
			DrawRequest currentDrawRequest = getNextDrawRequest();
			if (currentDrawRequest != null) {
				BlockRenderInfo[] progressiveRenderInfos = currentDrawRequest.getProgressiveRenderInfo();
				
				int block = 0;
				while (running && block < progressiveRenderInfos.length) {
					BlockRenderInfo blockRenderInfo = progressiveRenderInfos[block];
					mandelbrotApp.calculateMandelbrot(currentDrawRequest, blockRenderInfo.blockSize, blockRenderInfo.pixelOffsetX, blockRenderInfo.pixelOffsetY, blockRenderInfo.pixelSize);
					Platform.runLater(() -> {
						mandelbrotApp.drawMandelbrot();
					});

					DrawRequest anotherDrawRequest = getNextDrawRequest();
					if (anotherDrawRequest == null) {
						block++;
					} else {
						currentDrawRequest = anotherDrawRequest;
						progressiveRenderInfos = currentDrawRequest.getProgressiveRenderInfo();
						block = 0;
					}
				}
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
}