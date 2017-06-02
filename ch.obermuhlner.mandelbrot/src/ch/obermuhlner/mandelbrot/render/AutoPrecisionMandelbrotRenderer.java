package ch.obermuhlner.mandelbrot.render;

import java.math.BigDecimal;

import ch.obermuhlner.mandelbrot.javafx.Progress;
import ch.obermuhlner.mandelbrot.palette.Palette;
import javafx.scene.image.WritableImage;

public class AutoPrecisionMandelbrotRenderer implements MandelbrotRenderer {

	private static final BigDecimal DOUBLE_THRESHOLD = new BigDecimal("0.00000000002");

	private final MandelbrotRenderer lowPrecisionMandelbrotRenderer = new DoubleMandelbrotRenderer();
	
	private final MandelbrotRenderer highPrecisionMandelbrotRenderer = new BigDecimalMandelbrotRenderer();
	
	@Override
	public WritableImage drawMandelbrot(BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette, Progress progress) {
		if (xRadius.compareTo(DOUBLE_THRESHOLD) > 0 && yRadius.compareTo(DOUBLE_THRESHOLD) > 0) {
			return lowPrecisionMandelbrotRenderer.drawMandelbrot(xCenter, yCenter, xRadius, yRadius, precision, maxIterations, imageWidth, imageHeight, palette, progress);
		} else {
			return highPrecisionMandelbrotRenderer.drawMandelbrot(xCenter, yCenter, xRadius, yRadius, precision, maxIterations, imageWidth, imageHeight, palette, progress);
		}
	}

}
