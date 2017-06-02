package ch.obermuhlner.mandelbrot.palette;

public class HuePalette implements Palette {

	private final int steps;
	private final double saturation;
	private final double brightness;

	public HuePalette(int steps, double saturation, double brightness) {
		this.steps = steps;
		this.saturation = saturation;
		this.brightness = brightness;
	}
	
	@Override
	public Color getColor(int iterations) {
		int colorIndex = iterations % steps;
		double hue = 360.0 * colorIndex / steps;
		return Color.hsb(hue, saturation, brightness);
	}

}
