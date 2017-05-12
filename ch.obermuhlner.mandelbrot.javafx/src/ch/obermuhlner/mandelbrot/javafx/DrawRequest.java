package ch.obermuhlner.mandelbrot.javafx;

import java.math.BigDecimal;

public class DrawRequest {
	public final BigDecimal x;
	public final BigDecimal y;
	public final BigDecimal radius;

	public DrawRequest(BigDecimal x, BigDecimal y, BigDecimal radius) {
		this.x = x;
		this.y = y;
		this.radius = radius;
	}

	@Override
	public String toString() {
		return "DrawRequest [x=" + x + ", y=" + y + ", radius=" + radius + "]";
	}
}
