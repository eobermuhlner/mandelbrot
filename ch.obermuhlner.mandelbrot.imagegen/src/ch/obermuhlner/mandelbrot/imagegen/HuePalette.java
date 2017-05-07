package ch.obermuhlner.mandelbrot.imagegen;

import java.awt.Color;

public class HuePalette implements Palette {

	private int steps;

	public HuePalette(int steps) {
		this.steps = steps;
	}
	
	@Override
	public Color getColor(int iterations) {
		int colorIndex = iterations % steps;
		float hue = 360.0f * colorIndex / steps;
		return new Color(Color.HSBtoRGB(hue, 1.0f, 1.0f));
	}

}
