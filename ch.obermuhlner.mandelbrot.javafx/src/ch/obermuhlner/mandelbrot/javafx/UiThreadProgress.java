package ch.obermuhlner.mandelbrot.javafx;

import javafx.application.Platform;

public class UiThreadProgress implements Progress {

	private Progress decorated;

	public UiThreadProgress(Progress decorated) {
		this.decorated = decorated;
	}
	
	@Override
	public double getProgress() {
		return decorated.getProgress();
	}

	@Override
	public void setProgress(double progress) {
		Platform.runLater(() -> {
			decorated.setProgress(progress);
		});
	}

}
