package ch.obermuhlner.mandelbrot.cli;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import ch.obermuhlner.mandelbrot.javafx.Progress;
import ch.obermuhlner.mandelbrot.javafx.palette.CachingPalette;
import ch.obermuhlner.mandelbrot.javafx.palette.InterpolatingPalette;
import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import ch.obermuhlner.mandelbrot.javafx.palette.RandomPalette;
import ch.obermuhlner.mandelbrot.math.BigDecimalMath;
import ch.obermuhlner.mandelbrot.poi.PointOfInterest;
import ch.obermuhlner.mandelbrot.poi.StandardPointsOfInterest;
import ch.obermuhlner.mandelbrot.render.AutoPrecisionMandelbrotRenderer;
import ch.obermuhlner.mandelbrot.render.MandelbrotRenderer;
import ch.obermuhlner.mandelbrot.util.StopWatch;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

public class MandelbrotZoom {

	private static final MandelbrotRenderer mandelbrotRenderer = new AutoPrecisionMandelbrotRenderer();
	
	public static void main(String[] args) {
		if (args.length == 0) {
			renderZoomImagesPoi("Snail Shell", 100);
			return;
		}
		
		if ("-h".equals(args[0])) {
			System.out.println("Arguments: xCenter yCenter zoomStart zoomStep paletteStep imageCount directoryName");
			return;
		} else if ("-poi".equals(args[0])) {
			String poiName = stringArgument(args, 1, "Snail Shell");
			int imageCount = integerArgument(args, 2, 100);
			renderZoomImagesPoi(poiName, imageCount);
		} else {
			String xCenterString = stringArgument(args, 0, "0");
			String yCenterString = stringArgument(args, 1, "0");
			String zoomStartString = stringArgument(args, 2, "5");
			String zoomStepString = stringArgument(args, 3, "0.1");
			int paletteStep = integerArgument(args, 4, 10);
			int imageCount = integerArgument(args, 5, 100);
			String directoryName = stringArgument(args, 6, "zoom");
			
			renderZoomImages(xCenterString, yCenterString, zoomStartString, zoomStepString, paletteStep, imageCount, directoryName);		
		}
	}
	
	private static String stringArgument(String[] args, int index, String defaultValue) {
		if (args.length < index) {
			return args[index];
		} else {
			return defaultValue;
		}
	}
	
	private static int integerArgument(String[] args, int index, int defaultValue) {
		return Integer.parseInt(stringArgument(args, index, String.valueOf(defaultValue)));
	}
	
	private static void renderZoomImagesPoi(String pointOfInterestPattern, int imageCount) {
		for (PointOfInterest pointOfInterest : StandardPointsOfInterest.POINTS_OF_INTEREST) {
			if (pointOfInterestPattern.equals("") || pointOfInterestPattern.equals(pointOfInterest.name)) {
				renderZoomImages(
						pointOfInterest.x.toPlainString(),
						pointOfInterest.y.toPlainString(),
						"5",
						"0.1",
						pointOfInterest.paletteStep,
						imageCount,
						pointOfInterest.name);
			}
		}
	}

	public static void renderZoomImages(String xCenterString, String yCenterString, String zoomStartString, String zoomStepString, int paletteStep, int imageCount, String directoryName) {
		Path outDir = Paths.get("images", directoryName);
		outDir.toFile().mkdirs();

		Palette palette = new CachingPalette(new InterpolatingPalette(new RandomPalette(1), paletteStep));

		StopWatch stopWatch = new StopWatch();

		BigDecimal xCenter = new BigDecimal(xCenterString);
		BigDecimal yCenter = new BigDecimal(yCenterString);
		BigDecimal zoomStart = new BigDecimal(zoomStartString);
		BigDecimal zoomStep = new BigDecimal(zoomStepString);

		IntStream.range(0, imageCount).forEach(index -> {
			String filename = String.format("mandelbrot%04d.png", index);
			File file = outDir.resolve(filename).toFile();
			BigDecimal zoomPower = zoomStep.multiply(new BigDecimal(index));
			renderImage(file, xCenter, yCenter, zoomStart, zoomPower, palette);
		});

		System.out.println("Calculated all " + imageCount + " images for " + directoryName + " in " + stopWatch);
	}

	private static void renderImage(File file, BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomPower, Palette palette) {
		if (file.exists()) {
			System.out.println("Already calculated " + file.getName() + " with zoom " + zoomPower.toPlainString());
			return;
		}

		StopWatch stopWatch = new StopWatch();

		int precision = zoomPower.intValue() * 1 + 10;
		MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
		BigDecimal radius = zoomStart.multiply(BigDecimalMath.tenToThePowerOf(zoomPower.negate(), mc));
		int maxIterations = 1000 + zoomPower.intValue() * 100;
		int imageWidth = 800;
		int imageHeight = 800;

		Progress progress = new Progress() {
			@Override
			public void setTotalProgress(double totalProgress) {
				// ignore
			}
			@Override
			public void incrementProgress(double progress) {
				// ignore
			}
			@Override
			public double getProgress() {
				return 1.0;
			}
		};
		
		WritableImage image = mandelbrotRenderer.drawMandelbrot(
				xCenter,
				yCenter,
				radius,
				radius,
				precision,
				maxIterations,
				imageWidth,
				imageHeight,
				palette,
				progress);
		
		try {
			System.out.println("Calculated " + file.getName() + " with zoom " + zoomPower.toPlainString() + " in " + stopWatch);
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
