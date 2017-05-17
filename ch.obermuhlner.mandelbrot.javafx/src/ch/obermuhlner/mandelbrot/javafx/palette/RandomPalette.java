package ch.obermuhlner.mandelbrot.javafx.palette;

import java.util.Random;

import javafx.scene.paint.Color;

public class RandomPalette implements Palette {

	private final long seed;
	
	private final float hueStart;
	private final float hueEnd;
	private final float saturationStart;
	private final float saturationEnd;
	private final float brightnessStart;
	private final float brightnessEnd;
	
	public RandomPalette(long seed) {
		this(seed, 0f, 360f, 0.8f, 1.0f, 0.2f, 1.0f);
	}

	public RandomPalette(long seed, float hueStart, float hueEnd, float saturationStart, float saturationEnd, float brightnessStart, float brightnessEnd) {
		this.seed = seed;
		
		this.hueStart = hueStart;
		this.hueEnd = hueEnd;
		
		this.saturationStart = saturationStart;
		this.saturationEnd = saturationEnd;
		
		this.brightnessStart = brightnessStart;
		this.brightnessEnd = brightnessEnd;
	}

	@Override
	public Color getColor(int iterations) {
		return getRandomColor(iterations);
	}
	
	private Color getRandomColor(int index) {
		Random random = new Random(seed + index);
		random.nextDouble();
		float hue = random.nextFloat() * (hueEnd - hueStart) + hueStart;
		float saturation = random.nextFloat() * (saturationEnd - saturationStart) + saturationStart;
		float brightness = random.nextFloat() * (brightnessEnd - brightnessStart) + brightnessStart;
		return Color.hsb(hue, saturation, brightness);
	}

}
