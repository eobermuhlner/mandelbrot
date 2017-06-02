package ch.obermuhlner.mandelbrot.palette;

public class CyclingPalette implements Palette {

	private Color[] colors;

	public CyclingPalette(Color... colors) {
		this.colors = colors;
	}
	
	public CyclingPalette(Color repeatColor, int steps, Color... colors) {
		this.colors = createColors(repeatColor, steps, colors);
	}
	
	@Override
	public Color getColor(int iterations) {
		int index = iterations % colors.length;
		return colors[index];
	}

	private static Color[] createColors(Color repeatColor, int steps, Color... colors) {
		Color[] result = new Color[steps + colors.length];
		for (int i = 0; i < steps; i++) {
			result[i] = repeatColor;
		}
		for (int i = 0; i < colors.length; i++) {
			result[steps + i] = colors[i];
		}
		return result;
	}
}
