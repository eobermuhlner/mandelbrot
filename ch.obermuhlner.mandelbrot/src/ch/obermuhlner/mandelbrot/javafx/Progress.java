package ch.obermuhlner.mandelbrot.javafx;

public interface Progress {

	void setTotalProgress(double totalProgress);
	
	double getProgress();
	
	void incrementProgress(double progress);
}
