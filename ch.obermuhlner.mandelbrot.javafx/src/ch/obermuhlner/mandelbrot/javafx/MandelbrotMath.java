package ch.obermuhlner.mandelbrot.javafx;

import java.math.BigDecimal;
import java.math.MathContext;

import ch.obermuhlner.mandelbrot.math.BigDecimalMath;

public class MandelbrotMath {

	private static final BigDecimal TWO = new BigDecimal(2);

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
	
	public static int getMaxIteration(double zoom) {
		return (int) (zoom * 100 + 1000);
	}


}
