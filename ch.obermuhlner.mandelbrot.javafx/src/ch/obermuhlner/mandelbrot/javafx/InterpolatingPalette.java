package ch.obermuhlner.mandelbrot.javafx;

import java.util.Random;

import javafx.scene.paint.Color;

public class InterpolatingPalette implements Palette {

	private final Palette palette;
	private final int steps;
	
	public InterpolatingPalette(Palette palette, int steps) {
		this.palette = palette;
		this.steps = steps;
	}
	
	@Override
	public Color getColor(int iterations) {
		int colorIndex = iterations / steps;
		
		Color startColor = palette.getColor(colorIndex);
		Color endColor = palette.getColor(colorIndex + 1);

		double fraction = (double) (iterations % steps) / steps;
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
