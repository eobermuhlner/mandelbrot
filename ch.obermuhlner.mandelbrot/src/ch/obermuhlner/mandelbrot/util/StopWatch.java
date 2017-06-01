package ch.obermuhlner.mandelbrot.util;

public class StopWatch {
	private long startTime = System.nanoTime();

	public double getElapsedMilliseconds() {
		long endTime = System.nanoTime();
		
		return (endTime - startTime) / 1000000.0;
	}

	@Override
	public String toString() {
		return getElapsedMilliseconds() + " ms";
	}
}
