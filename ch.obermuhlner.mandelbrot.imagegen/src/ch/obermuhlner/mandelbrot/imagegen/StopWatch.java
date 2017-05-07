package ch.obermuhlner.mandelbrot.imagegen;

public class StopWatch {
	private long startTime = System.nanoTime();

	double getElapsedMilliseconds() {
		long endTime = System.nanoTime();
		
		return (endTime - startTime) / 1000000.0;
	}

	@Override
	public String toString() {
		return getElapsedMilliseconds() + " ms";
	}
}
