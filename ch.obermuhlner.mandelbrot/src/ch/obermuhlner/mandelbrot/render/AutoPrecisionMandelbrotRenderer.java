package ch.obermuhlner.mandelbrot.render;

import java.math.BigDecimal;

import ch.obermuhlner.mandelbrot.javafx.Progress;

public class AutoPrecisionMandelbrotRenderer implements MandelbrotRenderer {

	private static final BigDecimal DOUBLE_THRESHOLD = new BigDecimal("0.00000000002");

	private final MandelbrotRenderer lowPrecisionMandelbrotRenderer = new DoubleMandelbrotRenderer();
	
	private final MandelbrotRenderer highPrecisionMandelbrotRenderer = new BigDecimalMandelbrotRenderer();
	
	@Override
	public void drawMandelbrot(MandelbrotResult result, BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Progress progress) {
		if (xRadius.compareTo(DOUBLE_THRESHOLD) > 0 && yRadius.compareTo(DOUBLE_THRESHOLD) > 0) {
			lowPrecisionMandelbrotRenderer.drawMandelbrot(result, xCenter, yCenter, xRadius, yRadius, precision, maxIterations, imageWidth, imageHeight, progress);
		} else {
			highPrecisionMandelbrotRenderer.drawMandelbrot(result, xCenter, yCenter, xRadius, yRadius, precision, maxIterations, imageWidth, imageHeight, progress);
		}
	}

}
