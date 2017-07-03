package ch.obermuhlner.mandelbrot.palette;

public class FixPalette implements Palette {

	private final Palette palette;
	private final Color[] colors;

	public FixPalette(Palette palette, Color... colors) {
		this.palette = palette;
		this.colors = colors;
		
	}
	
	@Override
	public Color getColor(int iterations) {
		if (iterations < colors.length) {
			return colors[iterations];
		}
		
		return palette.getColor(iterations);
	}

}
