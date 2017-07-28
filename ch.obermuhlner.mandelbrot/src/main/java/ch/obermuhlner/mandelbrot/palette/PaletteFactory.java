package ch.obermuhlner.mandelbrot.palette;

public class PaletteFactory {

	private static final int LOG_INTERPOLATION_STEPS = 30;
	private static final Color[] FIX_COLORS = { Color.BLACK };
	
	public Palette createPalette(PaletteType paletteType, int seed, int steps) {
		switch (paletteType) {
		case RandomColor:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new RandomPalette(seed), FIX_COLORS), steps)));
		case RandomGray:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new RandomPalette(seed, 0f, 360f, 0.0f, 0.0f, 0.2f, 1.0f), FIX_COLORS), steps)));
		case RandomPastell:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new RandomPalette(seed, 0f, 360f, 0.0f, 0.3f, 0.2f, 1.0f), FIX_COLORS), steps)));
		case LogRandomColor:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new LogPalette(new FixPalette(new RandomPalette(seed), FIX_COLORS), steps), LOG_INTERPOLATION_STEPS)));
		case LogRandomGray:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new LogPalette(new FixPalette(new RandomPalette(seed, 0f, 360f, 0.0f, 0.0f, 0.2f, 1.0f), FIX_COLORS), steps), LOG_INTERPOLATION_STEPS)));
		case Drawing:
			return new MaxValuePalette(new CyclingPalette(Color.WHITE, steps, Color.gray(0.8), Color.gray(0.6), Color.gray(0.4), Color.gray(0.2), Color.gray(0.0), Color.gray(0.2), Color.gray(0.4), Color.gray(0.6), Color.gray(0.8)));
		case Fire:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new CyclingPalette(Color.RED, Color.YELLOW, Color.DARKRED, Color.ORANGE, Color.gray(0.1)), FIX_COLORS), steps, seed)));
		case Water:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new CyclingPalette(Color.BLUE, Color.LIGHTBLUE, Color.DARKBLUE, Color.CYAN, Color.gray(0.1)), FIX_COLORS), steps, seed)));
		case Air:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new CyclingPalette(Color.LIGHTBLUE, Color.WHITE, Color.BLUE, Color.WHITE, Color.CYAN), FIX_COLORS), steps, seed)));
		case Earth:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new CyclingPalette(Color.SADDLEBROWN, Color.GREEN, Color.DARKGREEN, Color.BROWN, Color.SANDYBROWN), FIX_COLORS), steps, seed)));
		case Forest:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new CyclingPalette(Color.GREENYELLOW, Color.GREEN, Color.DARKGREEN, Color.LIGHTGREEN, Color.gray(0.1)), FIX_COLORS), steps, seed)));
		case StarryNight:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new FixPalette(new CyclingPalette(Color.DARKBLUE, Color.WHITE, Color.gray(0.1), Color.MIDNIGHTBLUE, Color.gray(0.1)), FIX_COLORS), steps, seed)));
		case Rainbow:
			return new MaxValuePalette(new CachingPalette(new HuePalette(steps, 0.8, 0.8)));
		}
		
		throw new IllegalArgumentException("Unknown palette type: " + paletteType);
	}
}
