package ch.obermuhlner.mandelbrot.palette;

public class InterpolatingPalette implements Palette {

	private final Palette palette;
	private final int steps;
	private final int offset;
	
	public InterpolatingPalette(Palette palette, int steps) {
		this(palette, steps, 0);
	}

	public InterpolatingPalette(Palette palette, int steps, int offset) {
		this.palette = palette;
		this.steps = steps;
		this.offset = offset;
	}

	@Override
	public Color getColor(int iterations) {
		int n = iterations + offset;
		int colorIndex = n / steps;
		
		Color startColor = palette.getColor(colorIndex);
		Color endColor = palette.getColor(colorIndex + 1);

		double fraction = (double) (n % steps) / steps;
		return startColor.interpolate(endColor, fraction);
	}
	
	int interpolate(int start, int end, int fraction, int fractionMax) {
		int result = start + (end - start) * fraction / fractionMax;
		
		if (result < 0) {
			return 0;
		}
		if (result > 255) {
			return 255;
		}
		return result;
	}
}
