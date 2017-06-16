package ch.obermuhlner.mandelbrot.javafx;

import java.math.BigDecimal;
import java.math.MathContext;

import ch.obermuhlner.mandelbrot.math.BigDecimalMath;

public class MandelbrotMath {

	private static final BigDecimal TWO = new BigDecimal(2);
	private static final BigDecimal FOUR = new BigDecimal(4);

	public static int getCoordinatesPrecision(double zoom) {
		return Math.max(3, (int) (1.0 * zoom + 4));
	}

	
	public static int getPrecision(double zoom) {
		return getCoordinatesPrecision(zoom) + 6;
	}

	public static BigDecimal getRadius(double zoom) {
		int precision = getPrecision(zoom);
		MathContext mathContext = new MathContext(precision);
		BigDecimal radius = TWO.multiply(BigDecimalMath.tenToThePowerOf(BigDecimal.valueOf(-zoom), mathContext));
		return radius;
	}
	
	public static int calculateMandelbrotIterations(double x0, double y0, int maxIteration) {
		int iterations = 0;

		double x = 0;
		double y = 0;
		double xx = 0;
		double yy = 0;
		
		while (xx + yy < 2*2 && iterations < maxIteration) {
			y = 2*x*y + y0;
			x = xx - yy + x0;
			iterations++;
			
			xx = x*x;
			yy = y*y;
		}
		
		return iterations;
	}

	public static int calculateMandelbrotIterations(BigDecimal x0, BigDecimal y0, int maxIterations, MathContext mc) {
		int iterations = 0;
		BigDecimal x = BigDecimal.ZERO;
		BigDecimal y = BigDecimal.ZERO;
		BigDecimal xx = BigDecimal.ZERO;
		BigDecimal yy = BigDecimal.ZERO;
		
		while (xx.add(yy, mc).compareTo(FOUR) < 0 && iterations < maxIterations) {
			y = TWO.multiply(x, mc).multiply(y, mc).add(y0, mc);
			x = xx.subtract(yy, mc).add(x0, mc);
			iterations++;
			
			xx = x.multiply(x, mc);
			yy = y.multiply(y, mc);
		}
		
		return iterations;
	}
}
