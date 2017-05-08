package ch.obermuhlner.mandelbrot.imagegen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

// http://www.apfloat.org/apfloat_java/tutorial.html

public class MandelbrotImageGenerator {

	private static final Apfloat TWO = new Apfloat(2);

	private static final Apfloat TWO_SQUARE = new Apfloat(2*2);

	public static void main(String[] args) {
		//renderZoomImages("-0.04729622199", "-0.66103581600", "0.005", 1000, "zoom2");
		//renderZoomImages("-0.04729622199", "-0.66103581600", "1", 10, "zoom2");
		renderZoomImages("0.7436438885706", "0.1318259043124", "0.01", 1000, "zoom3");
		
	}
	
	public static void renderZoomImages(String xCenterString, String yCenterString, String zoomStepString, int imageCount, String directoryName) {
		Path outDir = Paths.get("images", directoryName);
		outDir.toFile().mkdirs();

		Palette palette = new CachingPalette(new RandomPalette(1, 10)); 

		StopWatch stopWatch = new StopWatch();

		Apfloat xCenter = new Apfloat(xCenterString);
		Apfloat yCenter = new Apfloat(yCenterString);
		Apfloat zoomStep = new Apfloat(zoomStepString, 10);

		IntStream.range(0, imageCount).parallel().forEach(index -> {
			String filename = String.format("mandelbrot%04d.png", index);
			File file = outDir.resolve(filename).toFile();
			Apfloat zoomPower = zoomStep.multiply(new Apfloat(index));
			renderImage(file, xCenter, yCenter, zoomPower, palette);
		});

		System.out.println("Calculated all " + imageCount + " images for " + directoryName + " in " + stopWatch);
	}

	private static void renderImage(File file, Apfloat xCenter, Apfloat yCenter, Apfloat zoomPower, Palette palette) {
		if (file.exists()) {
			System.out.println("Already calculated " + file.getName() + " with zoom " + zoomPower.toString(true));
			return;
		}

		StopWatch stopWatch = new StopWatch();

		int precision = zoomPower.intValue() + 10;
		Apfloat radius = ApfloatMath.pow(new Apfloat(10, precision), zoomPower.negate().precision(precision));
		int maxIterations = 2000;
		int imageWidth = 800;
		int imageHeight = 800;

		BufferedImage image = drawMandelbrot(
				xCenter.precision(precision),
				yCenter.precision(precision),
				radius,
				radius,
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

	private static BufferedImage drawMandelbrot(Apfloat xCenter, Apfloat yCenter, Apfloat xRadius, Apfloat yRadius, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		Apfloat threshold = new Apfloat("0.00000000000001");
		if (xRadius.compareTo(threshold) > 0 && yRadius.compareTo(threshold) > 0) {
			return drawMandelbrotDouble(xCenter.doubleValue(), yCenter.doubleValue(), xRadius.doubleValue(), yRadius.doubleValue(), maxIterations, imageWidth, imageHeight, palette);
		} else {
			return drawMandelbrotApfloat(xCenter, yCenter, xRadius, yRadius, maxIterations, imageWidth, imageHeight, palette);
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
				//System.out.print(pixelX + " " + pixelY);
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
				//System.out.println(" : " + iterations);
				gc.fillRect(pixelX, pixelY, 1, 1);
			}
		}
		
		return image;
	}
}
