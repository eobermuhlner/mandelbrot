package ch.obermuhlner.mandelbrot.palette;

public class LogPalette implements Palette {

	private final Palette palette;
	private final int logSteps;

	public LogPalette(Palette palette, int logSteps) {
		this.palette = palette;
		this.logSteps = logSteps;
	}
	
	@Override
	public Color getColor(int iterations) {
		int logIterations = (int) (Math.log10(iterations) * logSteps);
		return palette.getColor(logIterations);
	}

}
