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

import ch.obermuhlner.mandelbrot.math.BigDecimalMath;

// http://www.apfloat.org/apfloat_java/tutorial.html

// ffmpeg -y -r 60 -start_number 0 -i mandelbrot%04d.png -s 800x800 -vcodec mpeg4 -q:v 1 mandelbrot_fast.mp4


public class MandelbrotImageGenerator {

	private static final BigDecimal TWO = new BigDecimal(2);
	private static final BigDecimal TWO_SQUARE = new BigDecimal(2*2);

	private static final BigDecimal DOUBLE_THRESHOLD = new BigDecimal("0.00000000002");

	public static void main(String[] args) {
//		renderZoomImages(
//		"1.7497219297423385717104386951868287166821567562234308144552652007051737509332",
//		"-0.0000290166477536876274764422704374969315895481370276256407423549503316886795",
//		"5",
//		"0.01",
//		10,
//		1500,
//		"zoom1");

		//renderZoomImages("-0.04729622199", "-0.66103581600", "5", "0.005", 1000, 10, "zoom2");
		//renderZoomImages("0.7436438885706", "0.1318259043124", "5", "0.01", 1000, 10, "zoom3");
		//renderZoomImages("1.3482970614556051", "0.04900840524463914", "5", "0.01", 20, 1000, "zoom4");
		//renderZoomImages("-0.3277232503080546", "0.037120106058309704", "5", "0.1", 20, 133, "zoom5");
		//renderZoomImages("1.6206014961291328", "0.006846323168828212", "5", "0.01", 20, 1200, "zoom6.1");
		//renderZoomImages("1.6206014961291328", "0.006846323168828212", "5", "0.005", 10, 2200, "zoom6.2");
		
		//renderZoomImages("-0.26345476786999406", "-0.0027125008489098756", "5", "1", 10, 16, "zoom7_steps");
		//renderZoomImages("-0.26345476786999406", "-0.0027125008489098756", "5", "0.005", 10, 2300, "zoom7");

		// 0.049882468660064516 0.6745302994618768
		
		// 0.6156881882771636231743241163427195861 0.674900407359391227191516992928547498

		// very deep zoom from youtube
		// 1.740062382579339905220844167065825638296641720436171866879862418461182919644153056054840718339483225743450008259172138785492983677893366503417299549623738838303346465461290768441055486136870719850559269507357211790243666940134793753068611574745943820712885258222629105433648695946003865
		// 0.0281753397792110489924115211443195096875390767429906085704013095958801743240920186385400814658560553615695084486774077000669037710191665338060418999324320867147028768983704831316527873719459264592084600433150333362859318102017032958074799966721030307082150171994798478089798638258639934

		// very promising
		// 0.017919288259557892593847458731183211530081342400985
		// 1.011760975319870618534630909564772864062575481894402
		
		// first own deep zoom:
		// 1.6287436846258729580610678388260222631726264724258645100203844759630133245709586211806133286205124241241813
		// 0.0332156753545004949728396914777113920345351956506525205222378018672436406655074779379413131235765564138898

		// second deep zoom
		// 0.1739728951498616963982454198157626114689177807861745241100
		// 1.0873453915892155149725651368666330505026663476226736229695
	}
	
	public static void renderZoomImages(String xCenterString, String yCenterString, String zoomStartString, String zoomStepString, int paletteStep, int imageCount, String directoryName) {
		Path outDir = Paths.get("images", directoryName);
		outDir.toFile().mkdirs();

		Palette palette = new CachingPalette(new RandomPalette(1, paletteStep)); 

		StopWatch stopWatch = new StopWatch();

		BigDecimal xCenter = new BigDecimal(xCenterString);
		BigDecimal yCenter = new BigDecimal(yCenterString);
		BigDecimal zoomStart = new BigDecimal(zoomStartString);
		BigDecimal zoomStep = new BigDecimal(zoomStepString);

		IntStream.range(0, imageCount).parallel().forEach(index -> {
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

		BufferedImage image = drawMandelbrot(
				xCenter,
				yCenter,
				radius,
				radius,
				precision,
				maxIterations,
				imageWidth,
				imageHeight,
				palette);
		
		try {
			System.out.println("Calculated " + file.getName() + " with zoom " + zoomPower.toPlainString() + " in " + stopWatch);
			ImageIO.write(image, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static BufferedImage drawMandelbrot(BigDecimal xCenter, BigDecimal yCenter, BigDecimal xRadius, BigDecimal yRadius, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		if (xRadius.compareTo(DOUBLE_THRESHOLD) > 0 && yRadius.compareTo(DOUBLE_THRESHOLD) > 0) {
			return drawMandelbrotDouble(xCenter.doubleValue(), yCenter.doubleValue(), xRadius.doubleValue(), yRadius.doubleValue(), maxIterations, imageWidth, imageHeight, palette);
		} else {
			return drawMandelbrotBigDecimal(xCenter, yCenter, xRadius, yRadius, precision, maxIterations, imageWidth, imageHeight, palette);
		}
	}
	
	private static BufferedImage drawMandelbrotDouble(double xCenter, double yCenter, double xRadius, double yRadius, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D gc = image.createGraphics();

		double stepX = xRadius*2 / imageWidth;
		double stepY = yRadius*2 / imageHeight;
		double x0 = 0 - xCenter - xRadius; 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			double y0 = 0 - yCenter - yRadius; 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
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

				y0 += stepY;
			}
			x0 += stepX;
		}

		return image;
	}	
	
	private static BufferedImage drawMandelbrotBigDecimal(BigDecimal xCenterBigDecimal, BigDecimal yCenterBigDecimal, BigDecimal xRadiusBigDecimal, BigDecimal yRadiusBigDecimal, int precision, int maxIterations, int imageWidth, int imageHeight, Palette palette) {
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D gc = image.createGraphics();
		
		MathContext mc = new MathContext(precision, RoundingMode.HALF_EVEN);
		
		BigDecimal xCenter = new BigDecimal(xCenterBigDecimal.toString());
		BigDecimal yCenter = new BigDecimal(yCenterBigDecimal.toString());
		BigDecimal xRadius = new BigDecimal(xRadiusBigDecimal.toString());
		BigDecimal yRadius = new BigDecimal(xRadiusBigDecimal.toString());

		BigDecimal stepX = xRadius.multiply(TWO, mc).divide(BigDecimal.valueOf(imageWidth), mc);
		BigDecimal stepY = yRadius.multiply(TWO, mc).divide(BigDecimal.valueOf(imageHeight), mc);
		BigDecimal x0 = xCenter.negate().subtract(xRadius, mc); 
		
		for (int pixelX = 0; pixelX < imageWidth; pixelX++) {
			BigDecimal y0 = yCenter.negate().subtract(yRadius, mc); 
			for (int pixelY = 0; pixelY < imageHeight; pixelY++) {
				BigDecimal x = BigDecimal.ZERO;
				BigDecimal y = BigDecimal.ZERO;
				int iterations = 0;
				BigDecimal xx = x.multiply(x, mc);
				BigDecimal yy = y.multiply(y, mc);
				while (xx.add(yy, mc).compareTo(TWO_SQUARE) < 0 && iterations < maxIterations) {
					y = TWO.multiply(x, mc).multiply(y, mc).add(y0, mc);
					x = xx.subtract(yy, mc).add(x0, mc);
					iterations++;
					
					xx = x.multiply(x, mc);
					yy = y.multiply(y, mc);
				}

				Color color = iterations == maxIterations ? Color.BLACK : palette.getColor(iterations);
				gc.setColor(color);
				gc.fillRect(pixelX, pixelY, 1, 1);

				y0 = y0.add(stepY);
			}
			x0 = x0.add(stepX);
		}
		
		return image;
	}
}
