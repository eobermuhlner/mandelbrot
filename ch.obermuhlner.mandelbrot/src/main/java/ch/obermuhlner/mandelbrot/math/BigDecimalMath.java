package ch.obermuhlner.mandelbrot.math;

import java.math.BigDecimal;
import java.math.MathContext;

public class BigDecimalMath {

	private static final double LOG10 = Math.log(10);

	public static BigDecimal log10(BigDecimal value, MathContext mathContext) {
		double log10Mantissa = Math.log(mantissa(value).doubleValue()) / LOG10;
		return new BigDecimal(exponent(value)).add(BigDecimal.valueOf(log10Mantissa), mathContext);
	}
	
	public static BigDecimal tenToThePowerOf(BigDecimal value, MathContext mathContext) {
		if (value.signum() == 0) {
			return BigDecimal.ONE;
		}
		
		int exponent = value.intValue();
		double mantissa = value.subtract(new BigDecimal(exponent)).doubleValue();
		double tenToThePowerOfMantissa = Math.pow(10, mantissa);
		
		BigDecimal tenToThePowerOfExponent = tenToThePowerOf(exponent, mathContext);
		return tenToThePowerOfExponent.multiply(BigDecimal.valueOf(tenToThePowerOfMantissa), mathContext);
	}
	
	private static BigDecimal tenToThePowerOf(int value, MathContext mathContext) {
		if (value < 0) {
			return BigDecimal.ONE.divide(tenToThePowerOf(-value, mathContext));
		}
		return BigDecimal.TEN.pow(value);
	}

	public static BigDecimal mantissa(BigDecimal value) {
		int exponent = exponent(value);
		if (exponent == 0) {
			return value;
		}
		
		return value.movePointLeft(exponent);
	}

	public static int exponent(BigDecimal value) {
		return value.precision() - value.scale() - 1;
	}

}
