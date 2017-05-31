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
			renderZoomImagesPoi("Snail Shell");
			return;
		}
		
		if ("-h".equals(args[0])) {
			System.out.println("Arguments: xCenter yCenter zoomStart zoomStep paletteStep imageCount directoryName");
			return;
		} else if ("-poi".equals(args[0])) {
			renderZoomImagesPoi(args[1]);
		} else {
			String xCenterString = args[0];
			String yCenterString = args[1];
			String zoomStartString = args[2];
			String zoomStepString = args[3];
			int paletteStep = Integer.parseInt(args[4]);
			int imageCount = Integer.parseInt(args[5]);
			String directoryName = args[6];
			
			renderZoomImages(xCenterString, yCenterString, zoomStartString, zoomStepString, paletteStep, imageCount, directoryName);		
		}
		
	}
	
	private static void renderZoomImagesPoi(String pointOfInterestPattern) {
		for (PointOfInterest pointOfInterest : StandardPointsOfInterest.POINTS_OF_INTEREST) {
			if (pointOfInterestPattern.equals("") || pointOfInterestPattern.equals(pointOfInterest.name)) {
				renderZoomImages(
						pointOfInterest.x.toPlainString(),
						pointOfInterest.y.toPlainString(),
						"5",
						"0.1",
						pointOfInterest.paletteStep,
						120,
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
