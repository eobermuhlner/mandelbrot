package ch.obermuhlner.mandelbrot.poi;

import java.math.BigDecimal;

public class PointOfInterest {
	public final String name;
	public final BigDecimal x;
	public final BigDecimal y;
	public final double zoom;
	public final int paletteSeed;
	public final int paletteStep;

	public PointOfInterest(String name, BigDecimal x, BigDecimal y, double zoom, int paletteSeed, int paletteStep) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.zoom = zoom;
		this.paletteSeed = paletteSeed;
		this.paletteStep = paletteStep;
	}
	
	@Override
	public String toString() {
		return name;
	}
}