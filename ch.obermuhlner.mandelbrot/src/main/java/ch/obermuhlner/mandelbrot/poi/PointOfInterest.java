package ch.obermuhlner.mandelbrot.poi;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Properties;

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
	
	public void save(File file) throws IOException {
		try(PrintWriter writer = new PrintWriter(new FileWriter(file))) {
			writer.println("# Mandelbrot");
			writer.println("# https://github.com/eobermuhlner/mandelbrot");
			writer.println();
			writer.println("version=1.0.0");
			writer.println("name=" + name);
			writer.println("x=" + x.toPlainString());
			writer.println("y=" + y.toPlainString());
			writer.println("zoom=" + zoom);
			writer.println("paletteType=" + paletteType);
			writer.println("paletteSeed=" + paletteSeed);
			writer.println("paletteStep=" + paletteStep);
		}
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static PointOfInterest load(File file) throws IOException {
		try(FileReader reader = new FileReader(file)) {
			Properties properties = new Properties();
			properties.load(reader);
			
			String version = properties.getProperty("version");
			if (!version.startsWith("1.")) {
				throw new IOException("Incompatible mandelbrot version: " + version);
			}
			
			return new PointOfInterest(
					properties.getProperty("name"),
					new BigDecimal(properties.getProperty("x")),
					new BigDecimal(properties.getProperty("y")),
					Double.parseDouble(properties.getProperty("zoom")),
					PaletteType.valueOf(properties.getProperty("paletteType")),
					Integer.parseInt(properties.getProperty("paletteSeed")),
					Integer.parseInt(properties.getProperty("paletteStep")));
		}
	}
}