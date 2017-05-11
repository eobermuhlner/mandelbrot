package ch.obermuhlner.mandelbrot.javafx;

public class DrawRequest {
	public final double x;
	public final double y;
	public final double radius;

	public DrawRequest(double x, double y, double radius) {
		this.x = x;
		this.y = y;
		this.radius = radius;
	}

	@Override
	public String toString() {
		return "DrawRequest [x=" + x + ", y=" + y + ", radius=" + radius + "]";
	}
}
