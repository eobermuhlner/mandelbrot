package ch.obermuhlner.mandelbrot.cli;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import ch.obermuhlner.mandelbrot.javafx.DummyProgress;
import ch.obermuhlner.mandelbrot.javafx.Progress;
import ch.obermuhlner.mandelbrot.math.BigDecimalMath;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.palette.PaletteFactory;
import ch.obermuhlner.mandelbrot.palette.PaletteType;
import ch.obermuhlner.mandelbrot.poi.PointOfInterest;
import ch.obermuhlner.mandelbrot.poi.StandardPointsOfInterest;
import ch.obermuhlner.mandelbrot.render.AutoPrecisionMandelbrotRenderer;
import ch.obermuhlner.mandelbrot.render.MandelbrotRenderer;
import ch.obermuhlner.mandelbrot.util.StopWatch;

// ffmpeg -y -r 10 -start_number 0 -i mandelbrot%04d.png -s 800x800 -vcodec mpeg4 -q:v 1 mandelbrot.mp4

public class MandelbrotZoom {

	private static final MandelbrotRenderer mandelbrotRenderer = new AutoPrecisionMandelbrotRenderer();
	
	public static void main(String[] args) {
		if (args.length == 0) {
			printHelp();
			return;
		}
		
		boolean allPointsOfInterest = false;
		BigDecimal xCenter = new BigDecimal("0");
		BigDecimal yCenter = new BigDecimal("0");
		BigDecimal zoomStart = new BigDecimal("5");
		BigDecimal zoomStep = new BigDecimal("0.01");
		PaletteType paletteType = PaletteType.RandomColor;
		int paletteSeed = 14;
		int paletteStep = 20;
		int imageCount = 1000;
		String directoryName = "zoom";
		
		int argumentIndex = 0;
		while(argumentIndex < args.length) {
		
			switch(args[argumentIndex]) {
			case "-h":
			case "-?":
			case "--help":
				printHelp();
				return;
				
			case "-a":
			case "--all":
				allPointsOfInterest = true;
				break;
			case "-p":
			case "--poi":
				String poiName = stringArgument(args, ++argumentIndex, "Snail Shell");
				PointOfInterest pointOfInterest = StandardPointsOfInterest.findPointOfInterest(poiName);
				if (pointOfInterest == null) {
					System.out.println("Point of interest not found: " + poiName);
					return;
				} 
				xCenter = pointOfInterest.x;
				yCenter = pointOfInterest.y;
				paletteType = pointOfInterest.paletteType;
				paletteSeed = pointOfInterest.paletteSeed;
				paletteStep = pointOfInterest.paletteStep;
				directoryName = pointOfInterest.name;
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
				zoomStep = bigDecimalArgument(args, ++argumentIndex, new BigDecimal("0.01"));
				break;
			case "-t":
			case "--paletteType":
				paletteType = PaletteType.valueOf(stringArgument(args, ++argumentIndex, "RandomColor"));
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
				imageCount = integerArgument(args, ++argumentIndex, 1000);
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
		
		if (allPointsOfInterest) {
			for (PointOfInterest pointOfInterest : StandardPointsOfInterest.POINTS_OF_INTEREST) {
				xCenter = pointOfInterest.x;
				yCenter = pointOfInterest.y;
				paletteType = pointOfInterest.paletteType;
				paletteSeed = pointOfInterest.paletteSeed;
				paletteStep = pointOfInterest.paletteStep;
				directoryName = pointOfInterest.name;

				printInfo(System.out, xCenter, yCenter, zoomStart, zoomStep, paletteType, paletteSeed, paletteStep, imageCount, directoryName);

				renderZoomImages(xCenter, yCenter, zoomStart, zoomStep, paletteType, paletteSeed, paletteStep, imageCount, directoryName);
				System.out.println();
			}
		} else {
			printInfo(System.out, xCenter, yCenter, zoomStart, zoomStep, paletteType, paletteSeed, paletteStep, imageCount, directoryName);

			renderZoomImages(xCenter, yCenter, zoomStart, zoomStep, paletteType, paletteSeed, paletteStep, imageCount, directoryName);
		}
	}
	
	private static void printHelp() {
		System.out.println("Options:");
		System.out.println("  -a");
		System.out.println("  --all");
		System.out.println("    Creates the zooms for all points of interests.");
		System.out.println("  -p");
		System.out.println("  --poi");
		System.out.println("    Point of interest name (defines x, y, palette, name).");
		System.out.println("  -x");
		System.out.println("    Center x coordinate.");
		System.out.println("  -y");
		System.out.println("    Center y coordinate.");
		System.out.println("  -i");
		System.out.println("  --zoomStart");
		System.out.println("    Start value of zoom.");
		System.out.println("  -z");
		System.out.println("  --zoomStep");
		System.out.println("    Step to increase the zoom level for every image.");
		System.out.println("  -t");
		System.out.println("  --paletteType");
		System.out.println("    The palette type.");
		System.out.println("    One of: " + Arrays.toString(PaletteType.values()));
		System.out.println("  -r");
		System.out.println("  --paletteSeed");
		System.out.println("    Random seed value for the palette (if applicable for the palette type).");
		System.out.println("  -s");
		System.out.println("  --paletteStep");
		System.out.println("    Number of steps used in the palette (if applicable for the palette type).");
		System.out.println("  -c");
		System.out.println("  --count");
		System.out.println("  --imageCount");
		System.out.println("    Number of images to create.");
		System.out.println("  -n");
		System.out.println("  --name");
		System.out.println("  --directoryName");
		System.out.println("    Name of the directory to store the created images.");
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
		
	public static void renderZoomImages(BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomStep, PaletteType paletteType, int paletteSeed, int paletteStep, int imageCount, String directoryName) {
		Path outDir = Paths.get("images", directoryName);
		outDir.toFile().mkdirs();

		try {
			try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outDir.resolve("readme.txt").toFile())))) {
				printInfo(out, xCenter, yCenter, zoomStart, zoomStep, paletteType, paletteSeed, paletteStep, imageCount, directoryName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		PaletteFactory paletteFactory = new PaletteFactory();
		Palette palette = paletteFactory.createPalette(paletteType, paletteSeed, paletteStep);

		StopWatch stopWatch = new StopWatch();

		IntStream.range(0, imageCount).forEach(index -> {
			String filename = String.format("mandelbrot%04d.png", index);
			File file = outDir.resolve(filename).toFile();
			BigDecimal zoomPower = zoomStep.multiply(new BigDecimal(index));
			renderImage(file, xCenter, yCenter, zoomStart, zoomPower, palette);
		});

		System.out.println("Calculated all " + imageCount + " images for " + directoryName + " in " + stopWatch);
	}

	private static void printInfo(PrintStream out, BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomStep, PaletteType paletteType, int paletteSeed, int paletteStep, int imageCount, String directoryName) {
		out.println("x :             " + xCenter);
		out.println("y :             " + yCenter);
		out.println("zoomStart :     " + zoomStart);
		out.println("zoomStep :      " + zoomStep);
		out.println("paletteType :   " + paletteType);
		out.println("paletteSeed :   " + paletteSeed);
		out.println("paletteStep :   " + paletteStep);
		out.println("imageCount :    " + imageCount);
		out.println("directoryName : " + directoryName);		
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

		Progress progress = new DummyProgress();
		
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		BufferedImageMandelbrotResult result = new BufferedImageMandelbrotResult(image, palette);
		mandelbrotRenderer.drawMandelbrot(
				result,
				xCenter,
				yCenter,
				radius,
				radius,
				precision,
				maxIterations,
				imageWidth,
				imageHeight,
				progress);
		
		try {
			System.out.println("Calculated " + file.getName() + " with zoom " + zoomPower.toPlainString() + " in " + stopWatch);
			ImageIO.write(image, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
