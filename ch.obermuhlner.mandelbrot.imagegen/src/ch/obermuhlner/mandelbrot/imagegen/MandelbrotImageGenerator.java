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
		Palette palette = new CachingPalette(new RandomPalette(1, 10)); 

		Apfloat zoomStep = new Apfloat("0.1", 10);
		Apfloat xCenter = new Apfloat("1.7497219297423385717104386951868287166821567562234308144552652007051737509332");
		Apfloat yCenter = new Apfloat("-0.0000290166477536876274764422704374969315895481370276256407423549503316886795");

		IntStream.range(0, 150).parallel().forEach(index -> {
			Apfloat zoomPower = zoomStep.multiply(new Apfloat(index));
			renderImage(index, xCenter, yCenter, zoomPower, palette);
		});
	}

	private static void renderImage(int index, Apfloat xCenter, Apfloat yCenter, Apfloat zoomPower, Palette palette) {
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
			String filename = String.format("mandelbrot%04d.png", index);
			System.out.println("Calculated " + filename + " with zoom " + zoomPower.toString(true) + " in " + stopWatch);
			ImageIO.write(image, "png", new File(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static BufferedImage drawMandelbrot(Apfloat xCenter, Apfloat yCenter, Apfloat xRadius, Apfloat yRadius, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
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
