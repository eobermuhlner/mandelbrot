package ch.obermuhlner.mandelbrot.render;

import java.math.BigDecimal;

import ch.obermuhlner.mandelbrot.javafx.Progress;

public interface MandelbrotRenderer {

	void drawMandelbrot(MandelbrotResult result, BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Progress progress);

}
