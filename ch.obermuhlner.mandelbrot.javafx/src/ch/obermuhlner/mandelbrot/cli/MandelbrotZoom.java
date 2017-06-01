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

// ffmpeg -y -r 10 -start_number 0 -i mandelbrot%04d.png -s 800x800 -vcodec mpeg4 -q:v 1 mandelbrot.mp4

public class MandelbrotZoom {

	private static final MandelbrotRenderer mandelbrotRenderer = new AutoPrecisionMandelbrotRenderer();
	
	public static void main(String[] args) {
		if (args.length == 0) {
			printHelp();
			return;
		}
		
		BigDecimal xCenter = new BigDecimal("0");
		BigDecimal yCenter = new BigDecimal("0");
		BigDecimal zoomStart = new BigDecimal("5");
		BigDecimal zoomStep = new BigDecimal("0.1");
		int paletteSeed= 14;
		int paletteStep = 20;
		int imageCount = 100;
		String directoryName = "zoom";
		
		int argumentIndex = 0;
		while(argumentIndex < args.length) {
		
			switch(args[argumentIndex]) {
			case "-h":
			case "-?":
			case "--help":
				printHelp();
				return;
				
			case "-p":
			case "--poi":
				String poiName = stringArgument(args, ++argumentIndex, "Snail Shell");
				PointOfInterest pointOfInterest = findPointOfInterest(poiName);
				if (pointOfInterest != null) {
					xCenter = pointOfInterest.x;
					yCenter = pointOfInterest.y;
					paletteSeed = pointOfInterest.paletteSeed;
					paletteStep = pointOfInterest.paletteStep;
					directoryName = pointOfInterest.name;
				}
				break;

			case "-x":
			case "--x":
				xCenter = bigDecimalArgument(args, ++argumentIndex, BigDecimal.ZERO);
				break;
			case "-y":
			case "--y":
				yCenter = bigDecimalArgument(args, ++argumentIndex, BigDecimal.ZERO);
				break;
			case "-i":
			case "--zoomStart":
				zoomStart = bigDecimalArgument(args, ++argumentIndex, new BigDecimal("5"));
				break;
			case "-z":
			case "--zoomStep":
				zoomStep = bigDecimalArgument(args, ++argumentIndex, new BigDecimal("0.1"));
				break;
			case "-r":
			case "--paletteSeed":
				paletteSeed = integerArgument(args, ++argumentIndex, 14);
				break;
			case "-s":
			case "--paletteStep":
				paletteSeed = integerArgument(args, ++argumentIndex, 20);
				break;
			case "-c":
			case "--count":
			case "--imageCount":
				imageCount = integerArgument(args, ++argumentIndex, 100);
				break;
			case "-n":
			case "--name":
			case "--directoryName":
				directoryName = stringArgument(args, ++argumentIndex, "zoom");
				break;
			default:
				System.out.println("Unknown option: " + args[argumentIndex]);
				return;
			}
			
			argumentIndex++;
		}
		
		Palette palette = new CachingPalette(new InterpolatingPalette(new RandomPalette(paletteSeed), paletteStep));

		renderZoomImages(xCenter, yCenter, zoomStart, zoomStep, palette, imageCount, directoryName);		
	}
	
	private static void printHelp() {
		System.out.println("Options:");
		System.out.println("  -p");
		System.out.println("  --poi              point of interest name (defines x, y, palette, name)");
		System.out.println("  -x                 center x coordinate");
		System.out.println("  -y                 center y coordinate");
		System.out.println("  -i");
		System.out.println("  --zoomStart        zoom start");
		System.out.println("  -z");
		System.out.println("  --zoomStep         zoom step");
		System.out.println("  -r");
		System.out.println("  --paletteSeed      palette random seed");
		System.out.println("  -s");
		System.out.println("  --paletteStep      palette step");
		System.out.println("  -c");
		System.out.println("  --count");
		System.out.println("  --imageCount       image count");
		System.out.println("  -n");
		System.out.println("  --name");
		System.out.println("  --directoryName    output directory name");
		System.out.println();
		System.out.println("Points of interest:");
		for (PointOfInterest pointOfInterest : StandardPointsOfInterest.POINTS_OF_INTEREST) {
			System.out.println("  " + pointOfInterest.name);
		}
	}

	private static String stringArgument(String[] args, int index, String defaultValue) {
		if (index < args.length) {
			return args[index];
		} else {
			return defaultValue;
		}
	}
	
	private static BigDecimal bigDecimalArgument(String[] args, int index, BigDecimal defaultValue) {
		return new BigDecimal(stringArgument(args, index, defaultValue.toString()));
	}

	private static int integerArgument(String[] args, int index, int defaultValue) {
		return Integer.parseInt(stringArgument(args, index, String.valueOf(defaultValue)));
	}
	
	private static PointOfInterest findPointOfInterest(String poiName) {
		for (PointOfInterest pointOfInterest : StandardPointsOfInterest.POINTS_OF_INTEREST) {
			if (pointOfInterest.name.equals(poiName)) {
				return pointOfInterest;
			}
		}
		
		return null;
	}
	
	public static void renderZoomImages(BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomStep, Palette palette, int imageCount, String directoryName) {
		Path outDir = Paths.get("images", directoryName);
		outDir.toFile().mkdirs();

		StopWatch stopWatch = new StopWatch();

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
