package ch.obermuhlner.mandelbrot.render;

import java.math.BigDecimal;

import ch.obermuhlner.mandelbrot.javafx.Progress;
import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import javafx.scene.image.WritableImage;

public interface MandelbrotRenderer {

	WritableImage drawMandelbrot(BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette, Progress progress);

}
