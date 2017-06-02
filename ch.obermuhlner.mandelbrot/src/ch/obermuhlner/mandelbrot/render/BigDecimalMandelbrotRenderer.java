package ch.obermuhlner.mandelbrot.render;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.stream.IntStream;

import ch.obermuhlner.mandelbrot.javafx.Progress;

public class BigDecimalMandelbrotRenderer implements MandelbrotRenderer {

	private static final BigDecimal TWO = new BigDecimal(2);
	private static final BigDecimal TWO_SQUARE = new BigDecimal(2*2);

	@Override
	public void drawMandelbrot(MandelbrotResult result, BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Progress progress) {
		MathContext mc = new MathContext(precision, RoundingMode.HALF_EVEN);
		
		BigDecimal stepX = xRadius.multiply(TWO, mc).divide(BigDecimal.valueOf(imageWidth), mc);
		BigDecimal stepY = yRadius.multiply(TWO, mc).divide(BigDecimal.valueOf(imageHeight), mc);
		BigDecimal x0Start = xCenter.negate().subtract(xRadius, mc); 
		
		IntStream.range(0, imageWidth).parallel().forEach(pixelX -> {
			BigDecimal x0 = x0Start.add(stepX.multiply(new BigDecimal(pixelX), mc), mc);
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

				iterations = iterations == maxIterations ? Integer.MAX_VALUE : iterations;
				result.setIterations(pixelX, pixelY, iterations);

				y0 = y0.add(stepY);
			}

			progress.incrementProgress(imageWidth);
		});
	}

}
