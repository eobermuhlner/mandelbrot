package ch.obermuhlner.mandelbrot.imagegen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

// http://www.apfloat.org/apfloat_java/tutorial.html

// ffmpeg -y -r 60 -start_number 0 -i mandelbrot%04d.png -s 800x800 -vcodec mpeg4 -q:v 1 mandelbrot_fast.mp4


public class MandelbrotImageGenerator {

	private static final Apfloat TWO = new Apfloat(2);
	private static final Apfloat TWO_SQUARE = new Apfloat(2*2);

	private static final Apfloat DOUBLE_THRESHOLD = new Apfloat("0.00000000002");

	public static void main(String[] args) {
//		renderZoomImages(
//				"1.7497219297423385717104386951868287166821567562234308144552652007051737509332",
//				"-0.0000290166477536876274764422704374969315895481370276256407423549503316886795",
//				"5",
//				"0.01",
//				10,
//				1500,
//				"zoom1");

		//renderZoomImages("-0.04729622199", "-0.66103581600", "5", "0.005", 1000, 10, "zoom2");
		//renderZoomImages("0.7436438885706", "0.1318259043124", "5", "0.01", 1000, 10, "zoom3");
		//renderZoomImages("1.3482970614556051", "0.04900840524463914", "5", "0.01", 20, 1000, "zoom4");
		//renderZoomImages("-0.3277232503080546", "0.037120106058309704", "5", "0.1", 20, 133, "zoom5");
		//renderZoomImages("1.6206014961291328", "0.006846323168828212", "5", "0.01", 20, 1200, "zoom6.1");
		//renderZoomImages("1.6206014961291328", "0.006846323168828212", "5", "0.005", 10, 2200, "zoom6.2");
		
		//renderZoomImages("-0.26345476786999406", "-0.0027125008489098756", "5", "1", 10, 16, "zoom7_steps");
		renderZoomImages("-0.26345476786999406", "-0.0027125008489098756", "5", "0.005", 10, 2400, "zoom7");

		// 0.049882468660064516 0.6745302994618768
		
		// 0.6156881882771636231743241163427195861 0.674900407359391227191516992928547498
	}
	
	public static void renderZoomImages(String xCenterString, String yCenterString, String zoomStartString, String zoomStepString, int paletteStep, int imageCount, String directoryName) {
		Path outDir = Paths.get("images", directoryName);
		outDir.toFile().mkdirs();

		Palette palette = new CachingPalette(new RandomPalette(1, paletteStep)); 

		StopWatch stopWatch = new StopWatch();

		Apfloat xCenter = new Apfloat(xCenterString);
		Apfloat yCenter = new Apfloat(yCenterString);
		Apfloat zoomStart = new Apfloat(zoomStartString, 10);
		Apfloat zoomStep = new Apfloat(zoomStepString, 10);

		IntStream.range(0, imageCount).parallel().forEach(index -> {
			String filename = String.format("mandelbrot%04d.png", index);
			File file = outDir.resolve(filename).toFile();
			Apfloat zoomPower = zoomStep.multiply(new Apfloat(index));
			renderImage(file, xCenter, yCenter, zoomStart, zoomPower, palette);
		});

		System.out.println("Calculated all " + imageCount + " images for " + directoryName + " in " + stopWatch);
	}

	private static void renderImage(File file, Apfloat xCenter, Apfloat yCenter, Apfloat zoomStart, Apfloat zoomPower, Palette palette) {
		if (file.exists()) {
			System.out.println("Already calculated " + file.getName() + " with zoom " + zoomPower.toString(true));
			return;
		}

		StopWatch stopWatch = new StopWatch();

		int precision = zoomPower.intValue() * 2 + 10;
		Apfloat radius = zoomStart.multiply(ApfloatMath.pow(new Apfloat(10, precision), zoomPower.negate().precision(precision)));
		int maxIterations = 1000 + zoomPower.intValue() * 100;
		int imageWidth = 800;
		int imageHeight = 800;

		BufferedImage image = drawMandelbrot(
				xCenter.precision(precision),
				yCenter.precision(precision),
				radius,
				radius,
				precision,
				maxIterations,
				imageWidth,
				imageHeight,
				palette);
		
		try {
			System.out.println("Calculated " + file.getName() + " with zoom " + zoomPower.toString(true) + " in " + stopWatch);
			ImageIO.write(image, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static BufferedImage drawMandelbrot(Apfloat xCenter, Apfloat yCenter, Apfloat xRadius, Apfloat yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		if (xRadius.compareTo(DOUBLE_THRESHOLD) > 0 && yRadius.compareTo(DOUBLE_THRESHOLD) > 0) {
			return drawMandelbrotDouble(xCenter.doubleValue(), yCenter.doubleValue(), xRadius.doubleValue(), yRadius.doubleValue(), maxIterations, imageWidth, imageHeight, palette);
		} else {
			return drawMandelbrotApfloat(xCenter, yCenter, xRadius, yRadius, maxIterations, imageWidth, imageHeight, palette);
			//return drawMandelbrotBigDecimal(xCenter, yCenter, xRadius, yRadius, precision, maxIterations, imageWidth, imageHeight, palette);
		}
	}
	
	private static BufferedImage drawMandelbrotDouble(double xCenter, double yCenter, double xRadius, double yRadius, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D gc = image.createGraphics();

		double stepX = xRadius*2 / imageWidth;
		double stepY = yRadius*2 / imageHeight;
		double x0 = 0 - xCenter - xRadius; 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			x0 += stepX;
			double y0 = 0 - yCenter - yRadius; 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
				y0 += stepY;
				double x = 0;
				double y = 0;
				int iterations = 0;
				double xx = x*x;
				double yy = y*y;
				while (xx + yy < 2*2 && iterations < maxIterations) {
					y = 2*x*y + y0;
					x = xx - yy + x0;
					iterations++;
					
					xx = x*x;
					yy = y*y;
				}

				Color color = iterations == maxIterations ? Color.BLACK : palette.getColor(iterations);
				gc.setColor(color);
				gc.fillRect(pixelX, pixelY, 1, 1);
			}
		}

		return image;
	}	
	
	private static BufferedImage drawMandelbrotApfloat(Apfloat xCenter, Apfloat yCenter, Apfloat xRadius, Apfloat yRadius, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D gc = image.createGraphics();
		
		Apfloat stepX = xRadius.multiply(TWO).divide(new Apfloat(imageWidth));
		Apfloat stepY = yRadius.multiply(TWO).divide(new Apfloat(imageHeight));
		Apfloat x0 = xCenter.negate().subtract(xRadius); 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			x0 = x0.add(stepX);
			Apfloat y0 = yCenter.negate().subtract(yRadius); 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
				y0 = y0.add(stepY);
				Apfloat x = Apfloat.ZERO;
				Apfloat y = Apfloat.ZERO;
				int iterations = 0;
				Apfloat xx = x.multiply(x);
				Apfloat yy = y.multiply(y);
				while (xx.add(yy).compareTo(TWO_SQUARE) < 0 && iterations < maxIterations) {
					y = TWO.multiply(x).multiply(y).add(y0);
					x = xx.subtract(yy).add(x0);
					iterations++;
					
					xx = x.multiply(x);
					yy = y.multiply(y);
				}

				Color color = iterations == maxIterations ? Color.BLACK : palette.getColor(iterations);
				gc.setColor(color);
				gc.fillRect(pixelX, pixelY, 1, 1);
			}
		}
		
		return image;
	}

	private static BufferedImage drawMandelbrotBigDecimal(Apfloat xCenterApfloat, Apfloat yCenterApfloat, Apfloat xRadiusApfloat, Apfloat yRadiusApfloat, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D gc = image.createGraphics();
		
		MathContext mc = new MathContext(precision, RoundingMode.HALF_EVEN);
		
		BigDecimal two = new BigDecimal(2);
		BigDecimal twoSquare = new BigDecimal(2*2);
		
		BigDecimal xCenter = new BigDecimal(xCenterApfloat.toString());
		BigDecimal yCenter = new BigDecimal(yCenterApfloat.toString());
		BigDecimal xRadius = new BigDecimal(xRadiusApfloat.toString());
		BigDecimal yRadius = new BigDecimal(xRadiusApfloat.toString());

		BigDecimal stepX = xRadius.multiply(two, mc).divide(BigDecimal.valueOf(imageWidth), mc);
		BigDecimal stepY = yRadius.multiply(two, mc).divide(BigDecimal.valueOf(imageHeight), mc);
		BigDecimal x0 = xCenter.negate().subtract(xRadius, mc); 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			x0 = x0.add(stepX);
			BigDecimal y0 = yCenter.negate().subtract(yRadius, mc); 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
				y0 = y0.add(stepY);
				BigDecimal x = BigDecimal.ZERO;
				BigDecimal y = BigDecimal.ZERO;
				int iterations = 0;
				BigDecimal xx = x.multiply(x, mc);
				BigDecimal yy = y.multiply(y, mc);
				while (xx.add(yy, mc).compareTo(twoSquare) < 0 && iterations < maxIterations) {
					y = two.multiply(x, mc).multiply(y, mc).add(y0, mc);
					x = xx.subtract(yy, mc).add(x0, mc);
					iterations++;
					
					xx = x.multiply(x, mc);
					yy = y.multiply(y, mc);
				}

				Color color = iterations == maxIterations ? Color.BLACK : palette.getColor(iterations);
				gc.setColor(color);
				gc.fillRect(pixelX, pixelY, 1, 1);
			}
		}
		
		return image;
	}
}
