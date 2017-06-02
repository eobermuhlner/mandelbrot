package ch.obermuhlner.mandelbrot.cli;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ch.obermuhlner.mandelbrot.palette.Color;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.render.MandelbrotResult;

public class BufferedImageMandelbrotResult implements MandelbrotResult {

	private final int width;
	private final int height;
	private final Palette palette;

	private final int data[];

	public BufferedImageMandelbrotResult(int width, int height, Palette palette) {
		this.width = width;
		this.height = height;
		this.palette = palette;

		data = new int[width * height];
	}
	
	public BufferedImage getImage() {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Color color = palette.getColor(data[x + y * width]);
				
				graphics.setColor(toAwtColor(color));
				graphics.drawRect(x, y, 1, 1);
			}
		}

		return image;
	}
	
	@Override
	public void setIterations(int pixelX, int pixelY, int iterations) {
		data[pixelX + pixelY * width] = iterations;
	}

	private java.awt.Color toAwtColor(Color color) {
		return new java.awt.Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
	}
}
