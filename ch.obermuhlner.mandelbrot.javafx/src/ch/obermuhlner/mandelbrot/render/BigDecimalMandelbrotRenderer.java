package ch.obermuhlner.mandelbrot.render;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import ch.obermuhlner.mandelbrot.javafx.Progress;
import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class BigDecimalMandelbrotRenderer implements MandelbrotRenderer {

	private static final BigDecimal TWO = new BigDecimal(2);
	private static final BigDecimal TWO_SQUARE = new BigDecimal(2*2);

	@Override
	public WritableImage drawMandelbrot(BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette, Progress progress) {
		WritableImage image = new WritableImage(imageWidth, imageHeight);
		PixelWriter pixelWriter = image.getPixelWriter();
		
		MathContext mc = new MathContext(precision, RoundingMode.HALF_EVEN);
		
		BigDecimal stepX = xRadius.multiply(TWO, mc).divide(BigDecimal.valueOf(imageWidth), mc);
		BigDecimal stepY = yRadius.multiply(TWO, mc).divide(BigDecimal.valueOf(imageHeight), mc);
		BigDecimal x0 = xCenter.negate().subtract(xRadius, mc); 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			BigDecimal y0 = yCenter.negate().subtract(yRadius, mc); 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
				BigDecimal x = BigDecimal.ZERO;
				BigDecimal y = BigDecimal.ZERO;
				int iterations = 0;
				BigDecimal xx = x.multiply(x, mc);
				BigDecimal yy = y.multiply(y, mc);
				while (xx.add(yy, mc).compareTo(TWO_SQUARE) < 0 && iterations < maxIterations) {
					y = TWO.multiply(x, mc).multiply(y, mc).add(y0, mc);
					x = xx.subtract(yy, mc).add(x0, mc);
					iterations++;
					
					xx = x.multiply(x, mc);
					yy = y.multiply(y, mc);
				}

				Color color = iterations == maxIterations ? Color.BLACK : palette.getColor(iterations);
				pixelWriter.setColor(pixelX, pixelY, color);

				y0 = y0.add(stepY);
			}
			x0 = x0.add(stepX);

			double progressValue = (double)pixelX / imageWidth;
			progress.setProgress(progressValue);
		}

		progress.setProgress(1.0);
		
		return image;
	}

}
