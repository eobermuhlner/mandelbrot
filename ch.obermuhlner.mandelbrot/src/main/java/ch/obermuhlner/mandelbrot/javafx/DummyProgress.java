package ch.obermuhlner.mandelbrot.javafx;

public class DummyProgress implements Progress {

	@Override
	public void setTotalProgress(double totalProgress) {
		// ignore
	}
	@Override
	public void incrementProgress(double progress) {
		// ignore
	}
	@Override
	public double getProgress() {
		return 1.0;
	}
}
