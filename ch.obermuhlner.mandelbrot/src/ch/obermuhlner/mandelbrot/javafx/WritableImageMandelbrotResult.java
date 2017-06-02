package ch.obermuhlner.mandelbrot.javafx;

import ch.obermuhlner.mandelbrot.palette.Color;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.render.MandelbrotResult;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class WritableImageMandelbrotResult implements MandelbrotResult {

	private final WritableImage image;
	private final Palette palette;
	private final PixelWriter pixelWriter;

	public WritableImageMandelbrotResult(WritableImage image, Palette palette) {
		this.image = image;
		this.palette = palette;
		
		pixelWriter = image.getPixelWriter();
	}
	
	public WritableImage getWritableImage() {
		return image;
	}
	
	@Override
	public void setIterations(int pixelX, int pixelY, int iterations) {
		Color color = palette.getColor(iterations);
		pixelWriter.setColor(pixelX, pixelY, ColorUtil.toJavafxColor(color));
	}
}
