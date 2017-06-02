package ch.obermuhlner.mandelbrot.render;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import ch.obermuhlner.mandelbrot.javafx.Progress;

public class DoubleMandelbrotRenderer implements MandelbrotRenderer {

	@Override
	public void drawMandelbrot(MandelbrotResult result, BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Progress progress) {
		drawMandelbrotDouble(result, xCenter.doubleValue(), yCenter.doubleValue(), xRadius.doubleValue(), yRadius.doubleValue(), maxIterations, imageWidth, imageHeight, progress);
	}

	private void drawMandelbrotDouble(MandelbrotResult result, double xCenter, double yCenter, double xRadius, double yRadius, int maxIterations, int imageWidth, int imageHeight, Progress progress) {
		double stepX = xRadius*2 / imageWidth;
		double stepY = yRadius*2 / imageHeight;
		double x0Start = -xCenter - xRadius; 
		
		IntStream.range(0, imageWidth).parallel().forEach(pixelX -> {
			double x0 = x0Start + stepX * pixelX;
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

				iterations = iterations == maxIterations ? Integer.MAX_VALUE : iterations;
				result.setIterations(pixelX, pixelY, iterations);

				y0 += stepY;
			}
			x0 += stepX;
			
			progress.incrementProgress(imageWidth);
		});
	}	

	
}
