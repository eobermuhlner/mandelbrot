package ch.obermuhlner.mandelbrot.javafx.palette;

import javafx.scene.paint.Color;

public class CyclingPalette implements Palette {

	private Color[] colors;

	public CyclingPalette(Color... colors) {
		this.colors = colors;
	}
	
	public CyclingPalette(Color singleColor, Color repeatColor, int steps) {
		this.colors = createColors(singleColor, repeatColor, steps);
	}
	
	@Override
	public Color getColor(int iterations) {
		int index = iterations % colors.length;
		return colors[index];
	}

	private static Color[] createColors(Color singleColor, Color repeatColor, int steps) {
		Color[] colors = new Color[steps + 1];
		colors[0] = singleColor;
		for (int i = 1; i < colors.length; i++) {
			colors[i] = repeatColor;
		}
		return colors;
	}
}
