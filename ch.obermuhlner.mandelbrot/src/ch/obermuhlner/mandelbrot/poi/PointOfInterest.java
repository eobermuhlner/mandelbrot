package ch.obermuhlner.mandelbrot.poi;

import java.math.BigDecimal;

import ch.obermuhlner.mandelbrot.palette.PaletteType;

public class PointOfInterest {
	public final String name;
	public final BigDecimal x;
	public final BigDecimal y;
	public final double zoom;
	public final PaletteType paletteType;
	public final int paletteSeed;
	public final int paletteStep;

	public PointOfInterest(String name, BigDecimal x, BigDecimal y, double zoom, PaletteType paletteType, int paletteSeed, int paletteStep) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.zoom = zoom;
		this.paletteType = paletteType;
		this.paletteSeed = paletteSeed;
		this.paletteStep = paletteStep;
	}
	
	@Override
	public String toString() {
		return name;
	}
}