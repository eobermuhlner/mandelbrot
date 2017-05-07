package ch.obermuhlner.mandelbrot.imagegen;

import java.awt.Color;
import java.util.Random;

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

		int fractionIndex = iterations % steps;
		
		int red = interpolate(startColor.getRed(), endColor.getRed(), fractionIndex, steps);
		int green = interpolate(startColor.getGreen(), endColor.getGreen(), fractionIndex, steps);
		int blue = interpolate(startColor.getBlue(), endColor.getBlue(), fractionIndex, steps);
		
		return new Color(red, green, blue);
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
		float hue = random.nextFloat() * 360;
		float saturation = random.nextFloat() * 0.2f + 0.8f;
		float brightness = random.nextFloat() * 0.8f + 0.2f;
		return new Color(Color.HSBtoRGB(hue, saturation, brightness));
	}

}
