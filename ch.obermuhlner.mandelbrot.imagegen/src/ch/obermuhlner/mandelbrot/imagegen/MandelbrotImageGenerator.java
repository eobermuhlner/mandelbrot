package ch.obermuhlner.mandelbrot.imagegen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

// http://www.apfloat.org/apfloat_java/tutorial.html

public class MandelbrotImageGenerator {

	private static final Apfloat TWO = new Apfloat(2);

	private static final Apfloat TWO_SQUARE = new Apfloat(2*2);

	public static void main(String[] args) {
		new File("images").mkdir();

		Palette palette = new CachingPalette(new RandomPalette(1, 10)); 

		Apfloat zoomStep = new Apfloat("0.01", 10);
		Apfloat xCenter = new Apfloat("1.7497219297423385717104386951868287166821567562234308144552652007051737509332");
		Apfloat yCenter = new Apfloat("-0.0000290166477536876274764422704374969315895481370276256407423549503316886795");

		IntStream.range(0, 1500).parallel().forEach(index -> {
			Apfloat zoomPower = zoomStep.multiply(new Apfloat(index));
			renderImage(index, xCenter, yCenter, zoomPower, palette);
		});
	}

	private static void renderImage(int index, Apfloat xCenter, Apfloat yCenter, Apfloat zoomPower, Palette palette) {
		String filename = String.format("images/mandelbrot%04d.png", index);
		File file = new File(filename);
		if (file.exists()) {
			return;
		}

		StopWatch stopWatch = new StopWatch();

		int precision = zoomPower.intValue() + 10;
		Apfloat radius = ApfloatMath.pow(new Apfloat(10, precision), zoomPower.negate().precision(precision));
		int maxIterations = 1000;
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
			System.out.println("Calculated " + filename + " with zoom " + zoomPower.toString(true) + " in " + stopWatch);
			ImageIO.write(image, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static BufferedImage drawMandelbrot(Apfloat xCenter, Apfloat yCenter, Apfloat xRadius, Apfloat yRadius, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		Apfloat threshold = new Apfloat("0.0000000001");
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
