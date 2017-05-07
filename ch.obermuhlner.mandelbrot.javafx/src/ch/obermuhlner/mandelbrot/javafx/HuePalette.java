package ch.obermuhlner.mandelbrot.javafx;

import javafx.scene.paint.Color;

public class HuePalette implements Palette {

	private int steps;

	public HuePalette(int steps) {
		this.steps = steps;
	}
	
	@Override
	public Color getColor(int iterations) {
		int colorIndex = iterations % steps;
		double hue = 360.0 * colorIndex / steps;
		return Color.hsb(hue, 1.0, 1.0);
	}

}
