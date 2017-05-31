package ch.obermuhlner.mandelbrot.javafx;

import javafx.application.Platform;

public class UiThreadProgress implements Progress {

	private Progress decorated;

	public UiThreadProgress(Progress decorated) {
		this.decorated = decorated;
	}

	@Override
	public void setTotalProgress(double totalProgress) {
		decorated.setTotalProgress(totalProgress);
	}
	
	@Override
	public double getProgress() {
		return decorated.getProgress();
	}

	@Override
	public void incrementProgress(double progress) {
		Platform.runLater(() -> {
			decorated.incrementProgress(progress);
		});
	}

}
