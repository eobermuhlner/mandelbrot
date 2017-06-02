package ch.obermuhlner.mandelbrot.cli;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ch.obermuhlner.mandelbrot.palette.Color;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.render.MandelbrotResult;

public class BufferedImageMandelbrotResult implements MandelbrotResult {

	private final BufferedImage image;
	private final Palette palette;
	private Graphics2D graphics;

	public BufferedImageMandelbrotResult(BufferedImage image, Palette palette) {
		this.image = image;
		this.palette = palette;
		
		graphics = image.createGraphics();
	}
	
	public BufferedImage getWritableImage() {
		return image;
	}
	
	@Override
	public void setIterations(int pixelX, int pixelY, int iterations) {
		Color color = palette.getColor(iterations);

		graphics.setColor(toAwtColor(color));
		graphics.drawRect(pixelX, pixelY, 1, 1);
	}

	private java.awt.Color toAwtColor(Color color) {
		return new java.awt.Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
	}
}
