package ch.obermuhlner.mandelbrot.javafx;

public interface Progress {

	double getProgress();
	
	void setProgress(double progress);
	
	default void setProgress(int stepsDone, int stepsTotal) {
		setProgress((double)stepsDone / stepsTotal);
	}
}
