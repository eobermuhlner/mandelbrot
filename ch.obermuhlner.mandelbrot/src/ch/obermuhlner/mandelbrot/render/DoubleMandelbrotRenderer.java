package ch.obermuhlner.mandelbrot.render;

import java.math.BigDecimal;

import ch.obermuhlner.mandelbrot.javafx.ColorUtil;
import ch.obermuhlner.mandelbrot.javafx.Progress;
import ch.obermuhlner.mandelbrot.palette.Color;
import ch.obermuhlner.mandelbrot.palette.Palette;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class DoubleMandelbrotRenderer implements MandelbrotRenderer {

	@Override
	public WritableImage drawMandelbrot(BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette, Progress progress) {
		return drawMandelbrotDouble(xCenter.doubleValue(), yCenter.doubleValue(), xRadius.doubleValue(), yRadius.doubleValue(), maxIterations, imageWidth, imageHeight, palette, progress);
	}

	private WritableImage drawMandelbrotDouble(double xCenter, double yCenter, double xRadius, double yRadius, int maxIterations, int imageWidth, int imageHeight, Palette palette, Progress progress) {
		WritableImage image = new WritableImage(imageWidth, imageHeight);
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double stepX = xRadius*2 / imageWidth;
		double stepY = yRadius*2 / imageHeight;
		double x0 = 0 - xCenter - xRadius; 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			double y0 = 0 - yCenter - yRadius; 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
				double x = 0;
				double y = 0;
				int iterations = 0;
				double xx = x*x;
				double yy = y*y;
				while (xx + yy < 2*2 && iterations < maxIterations) {
					y = 2*x*y + y0;
					x = xx - yy + x0;
					iterations++;
					
					xx = x*x;
					yy = y*y;
				}

				Color color = iterations == maxIterations ? Color.BLACK : palette.getColor(iterations);
				pixelWriter.setColor(pixelX, pixelY, ColorUtil.toJavafxColor(color));

				y0 += stepY;
			}
			x0 += stepX;
			
			progress.incrementProgress(imageWidth);
		}
		
		return image;
	}	

	
}
