package ch.obermuhlner.mandelbrot.cli;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import javax.imageio.ImageIO;

import ch.obermuhlner.mandelbrot.javafx.DummyProgress;
import ch.obermuhlner.mandelbrot.javafx.Progress;
import ch.obermuhlner.mandelbrot.javafx.WritableImageMandelbrotResult;
import ch.obermuhlner.mandelbrot.math.BigDecimalMath;
import ch.obermuhlner.mandelbrot.palette.CachingPalette;
import ch.obermuhlner.mandelbrot.palette.InterpolatingPalette;
import ch.obermuhlner.mandelbrot.palette.MaxValuePalette;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.palette.RandomPalette;
import ch.obermuhlner.mandelbrot.poi.PointOfInterest;
import ch.obermuhlner.mandelbrot.poi.StandardPointsOfInterest;
import ch.obermuhlner.mandelbrot.render.BigDecimalMandelbrotRenderer;
import javafx.embed.swing.SwingFXUtils;

/**
 * Creates the same image for every zoom level multiple times with various precisions.
 * This is useful to verify that the calculation at a specific zoom level are done with the optimum precision.
 * 
 * Experiments show that a precision of zoomLevel+5 should be accurate enough.
 */
public class PrecisionCheck {

	public static void main(String[] args) {
		precisionCheck("Thorns");
	}

	private static void precisionCheck(String poiName) {
		PointOfInterest pointOfInterest = StandardPointsOfInterest.findPointOfInterest(poiName);
		if (pointOfInterest == null) {
			System.out.println("Point of interest not found: " + poiName);
			return;
		} 
		BigDecimal xCenter = pointOfInterest.x;
		BigDecimal yCenter = pointOfInterest.y;
		int paletteSeed = pointOfInterest.paletteSeed;
		int paletteStep = pointOfInterest.paletteStep;
		
		BigDecimal zoomStart = new BigDecimal("5");
		Palette palette = new MaxValuePalette(new CachingPalette(new InterpolatingPalette(new RandomPalette(paletteSeed), paletteStep)));

		Progress progress = new DummyProgress();

		for (BigDecimal zoomPower = new BigDecimal(10); zoomPower.compareTo(new BigDecimal(100)) <= 0; zoomPower = zoomPower.add(new BigDecimal(10))) {
			for (int precisionOffset = 0; precisionOffset <= 10; precisionOffset++) {
				int precision = zoomPower.intValue() + precisionOffset;
				
				MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
				BigDecimal radius = zoomStart.multiply(BigDecimalMath.tenToThePowerOf(zoomPower.negate(), mc));
				
				BigDecimalMandelbrotRenderer mandelbrotRenderer = new BigDecimalMandelbrotRenderer();
				int maxIterations = 1000 + zoomPower.intValue() * 100;
				int imageSize = 100;
				
				File file = new File("check_zoom" + zoomPower + "_precision" + precision + ".png");
				if (!file.exists()) {
					WritableImageMandelbrotResult result = new WritableImageMandelbrotResult(imageSize, imageSize, palette);
					mandelbrotRenderer.drawMandelbrot(result, xCenter, yCenter, radius, radius, precision, maxIterations, imageSize, imageSize, progress);

					System.out.println("Calculating " + file);
					try {
						ImageIO.write(SwingFXUtils.fromFXImage(result.getImage(), null), "png", file);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}