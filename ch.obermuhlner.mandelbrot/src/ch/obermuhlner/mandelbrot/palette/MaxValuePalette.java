package ch.obermuhlner.mandelbrot.palette;

public class MaxValuePalette implements Palette {

	private final Palette palette;
	private final Color maxValueColor;

	public MaxValuePalette(Palette palette) {
		this(palette, Color.BLACK);
	}
	
	public MaxValuePalette(Palette palette, Color maxValueColor) {
		this.palette = palette;
		this.maxValueColor = maxValueColor;
	}
	
	@Override
	public Color getColor(int iterations) {
		if (iterations == Integer.MAX_VALUE) {
			return maxValueColor;
		}
		
		return palette.getColor(iterations);
	}

}
