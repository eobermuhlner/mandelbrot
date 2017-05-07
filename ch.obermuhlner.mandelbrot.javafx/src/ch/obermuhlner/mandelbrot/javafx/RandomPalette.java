package ch.obermuhlner.mandelbrot.javafx;

import java.util.Random;

import javafx.scene.paint.Color;

public class RandomPalette implements Palette {

	private long seed;
	private int steps;
	
	public RandomPalette(long seed, int steps) {
		this.seed = seed;
		this.steps = steps;
	}
	
	@Override
	public Color getColor(int iterations) {
		int colorIndex = iterations / steps;
		
		Color startColor = getRandomColor(colorIndex);
		Color endColor = getRandomColor(colorIndex + 1);

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
	
	private Color getRandomColor(int index) {
		Random random = new Random(seed + index);
		random.nextLong();
		double hue = random.nextDouble() * 360;
		double saturation = random.nextDouble() * 0.2f + 0.8f;
		double brightness = random.nextDouble() * 0.8f + 0.2f;
		return Color.hsb(hue, saturation, brightness);
	}

}
