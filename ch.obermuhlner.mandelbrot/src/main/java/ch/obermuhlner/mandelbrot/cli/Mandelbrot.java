package ch.obermuhlner.mandelbrot.cli;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class Mandelbrot {

	private static final MandelbrotRenderer mandelbrotRenderer = new AutoPrecisionMandelbrotRenderer();
	
	public static void main(String[] args) {
		if (args.length == 0) {
			printHelp();
			return;
		}
		
		List<String> fileNames = new ArrayList<>();
		
		BigDecimal xCenter = null;
		BigDecimal yCenter = null;
		BigDecimal zoom = null;
		//Function<BigDecimal, Integer> zoomLevelToMaxIterationsFunction = (zoomLevel) -> (int) (zoomLevel.doubleValue() * 1000 + 1000);
		PaletteType paletteType = null;
		Integer paletteSeed = null;
		Integer paletteStep = null;
		int width = 1920;
		int height = 1080;
		
		int argumentIndex = 0;
		while(argumentIndex < args.length) {
		
			switch(args[argumentIndex]) {
			case "-?":
			case "--help":
				printHelp();
				return;
				
			case "-x":
			case "--x":
				xCenter = bigDecimalArgument(args, ++argumentIndex, BigDecimal.ZERO);
				break;
			case "-y":
			case "--y":
				yCenter = bigDecimalArgument(args, ++argumentIndex, BigDecimal.ZERO);
				break;
			case "-z":
			case "--zoom":
				zoom = bigDecimalArgument(args, ++argumentIndex, new BigDecimal("5"));
				break;
			case "-t":
			case "--paletteType":
				paletteType = PaletteType.valueOf(stringArgument(args, ++argumentIndex, "RandomColor"));
				break;
			case "-r":
			case "--paletteSeed":
				paletteSeed = integerArgument(args, ++argumentIndex, 1);
				break;
			case "-s":
			case "--paletteStep":
				paletteSeed = integerArgument(args, ++argumentIndex, 20);
				break;
			case "-w":
			case "--width":
				width = integerArgument(args, ++argumentIndex, 1920);
				break;
			case "-h":
			case "--height":
				height = integerArgument(args, ++argumentIndex, 1080);
				break;
			default:
				String arg = args[argumentIndex];
				if (arg.startsWith("-")) {
					System.out.println("Unknown option: " + arg);
					return;
				}
				fileNames.add(arg);
			}
			
			argumentIndex++;
		}
		
		for (String fileName : fileNames) {
			try {
				PointOfInterest pointOfInterest = PointOfInterest.load(new File(fileName));
				if (xCenter != null) {
					pointOfInterest.x = xCenter;
				}
				if (yCenter != null) {
					pointOfInterest.y = yCenter;
				}
				if (zoom != null) {
					pointOfInterest.zoom = zoom.doubleValue(); // TODO make poi.zoom a BigDecimal
				}
				if (paletteType != null) {
					pointOfInterest.paletteType = paletteType;
				}
				if (paletteSeed != null) {
					pointOfInterest.paletteSeed = paletteSeed;
				}
				if (paletteStep != null) {
					pointOfInterest.paletteStep = paletteStep;
				}
				
				renderImage(pointOfInterest, width, height);
				System.out.println("Rendered " + fileName);
			} catch (IOException e) {
				System.out.println("Failed to load mandelbrot file: " + fileName);
				System.out.println(e.getMessage());
			}
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
		System.out.println("  -z");
		System.out.println("  --zoom");
		System.out.println("    Zoom level.");
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

	private static void renderImage(PointOfInterest poi, int width, int height) {
		PaletteFactory paletteFactory = new PaletteFactory();
		Palette palette = paletteFactory.createPalette(poi.paletteType, poi.paletteSeed, poi.paletteStep);
		double colorOffset = 0.0;

		File file = new File(poi.name + ".png");
		
		renderImage(
				file,
				width, 
				height,
				poi.x,
				poi.y,
				new BigDecimal(2),
				BigDecimal.valueOf(poi.zoom),
				palette,
				colorOffset);
	}
	
	private static void renderImage(File file, int imageWidth, int imageHeight, BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomPower, Palette palette, double colorOffset) {
		int precision = zoomPower.intValue() * 1 + 10;
		MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);

		Progress progress = new DummyProgress();

		int maxIterations = 1000 + zoomPower.intValue() * 1000;
		BigDecimal radius = zoomStart.multiply(BigDecimalMath.tenToThePowerOf(zoomPower.negate(), mc));
		BigDecimal minWidthHeight = new BigDecimal(Math.min(imageWidth, imageHeight));
		BigDecimal xRadius = radius.multiply(new BigDecimal(imageWidth), mc).divide(minWidthHeight, mc);
		BigDecimal yRadius = radius.multiply(new BigDecimal(imageHeight), mc).divide(minWidthHeight, mc);

		BufferedImageMandelbrotResult result = new BufferedImageMandelbrotResult(imageWidth, imageHeight, palette, colorOffset);
		mandelbrotRenderer.drawMandelbrot(
				result,
				xCenter,
				yCenter,
				xRadius,
				yRadius,
				precision,
				maxIterations,
				imageWidth,
				imageHeight,
				progress);
		
		try {
			ImageIO.write(result.getImage(), "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
