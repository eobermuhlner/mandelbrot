package ch.obermuhlner.mandelbrot.palette;

public class PaletteFactory {

	private static final int LOG_INTERPOLATION_STEPS = 30;
	
	public Palette createPalette(PaletteType paletteType, int seed, int steps) {
		switch (paletteType) {
		case RandomColor:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new RandomPalette(seed), steps)));
		case RandomGray:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new RandomPalette(seed, 0f, 360f, 0.0f, 0.0f, 0.2f, 1.0f), steps)));
		case RandomPastell:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new RandomPalette(seed, 0f, 360f, 0.0f, 0.3f, 0.2f, 1.0f), steps)));
		case LogRandomColor:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new LogPalette(new RandomPalette(seed), steps), LOG_INTERPOLATION_STEPS)));
		case LogRandomGray:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new LogPalette(new RandomPalette(seed, 0f, 360f, 0.0f, 0.0f, 0.2f, 1.0f), steps), LOG_INTERPOLATION_STEPS)));
		case Drawing:
			return new MaxValuePalette(new CyclingPalette(Color.WHITE, steps, Color.gray(0.8), Color.gray(0.6), Color.gray(0.4), Color.gray(0.2), Color.gray(0.0), Color.gray(0.2), Color.gray(0.4), Color.gray(0.6), Color.gray(0.8)));
		case Fire:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.RED, Color.YELLOW, Color.DARKRED, Color.ORANGE, Color.gray(0.1)), steps)));
		case Water:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.BLUE, Color.LIGHTBLUE, Color.DARKBLUE, Color.CYAN, Color.gray(0.1)), steps)));
		case Air:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.LIGHTBLUE, Color.WHITE, Color.BLUE, Color.WHITE, Color.CYAN), steps)));
		case Earth:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.SADDLEBROWN, Color.GREEN, Color.DARKGREEN, Color.BROWN, Color.SANDYBROWN), steps)));
		case Forest:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.GREENYELLOW, Color.GREEN, Color.DARKGREEN, Color.LIGHTGREEN, Color.gray(0.1)), steps)));
		case StarryNight:
			return new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.gray(0.1), Color.WHITE, Color.gray(0.1)), steps)));
		case Rainbow:
			return new MaxValuePalette(new CachingPalette(new HuePalette(steps, 0.8, 0.8)));
		}
		
		throw new IllegalArgumentException("Unknown palette type: " + paletteType);
	}
}
