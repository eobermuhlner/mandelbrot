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
	public String name;
	public BigDecimal x;
	public BigDecimal y;
	public double zoom;
	public PaletteType paletteType;
	public int paletteSeed;
	public int paletteStep;
	public int maxIterationsConst;
	public int maxIterationsLinear;

	public PointOfInterest(String name, BigDecimal x, BigDecimal y, double zoom, PaletteType paletteType, int paletteSeed, int paletteStep, int maxIterationsConst, int maxIterationsLinear) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.zoom = zoom;
		this.paletteType = paletteType;
		this.paletteSeed = paletteSeed;
		this.paletteStep = paletteStep;
		this.maxIterationsConst = maxIterationsConst;
		this.maxIterationsLinear = maxIterationsLinear;
	}
	
	public void save(File file) throws IOException {
		try(PrintWriter writer = new PrintWriter(new FileWriter(file))) {
			writer.println("# Mandelbrot");
			writer.println("# https://github.com/eobermuhlner/mandelbrot");
			writer.println();
			writer.println("version=1.1.0");
			writer.println("name=" + name);
			writer.println("x=" + x.toPlainString());
			writer.println("y=" + y.toPlainString());
			writer.println("zoom=" + zoom);
			writer.println("paletteType=" + paletteType);
			writer.println("paletteSeed=" + paletteSeed);
			writer.println("paletteStep=" + paletteStep);
			writer.println("maxIterationsConst=" + maxIterationsConst);
			if (maxIterationsLinear != 0) {
				writer.println("maxIterationsLinear=" + maxIterationsLinear);
			}
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
			
			String maxIterationsConstString = properties.getProperty("maxIterationsConst");
			String maxIterationsLinearString = properties.getProperty("maxIterationsLinear");
			
			if (maxIterationsConstString == null) {
				maxIterationsConstString = "1000";
				if (maxIterationsLinearString == null) {
					maxIterationsLinearString = "1000";
				}
			}
			if (maxIterationsLinearString == null) {
				maxIterationsLinearString = "0";
			}
			
			return new PointOfInterest(
					properties.getProperty("name"),
					new BigDecimal(properties.getProperty("x")),
					new BigDecimal(properties.getProperty("y")),
					Double.parseDouble(properties.getProperty("zoom")),
					PaletteType.valueOf(properties.getProperty("paletteType")),
					Integer.parseInt(properties.getProperty("paletteSeed")),
					Integer.parseInt(properties.getProperty("paletteStep")),
					Integer.parseInt(maxIterationsConstString),
					Integer.parseInt(maxIterationsLinearString));
		}
	}

	public BigDecimal distanceSquare(PointOfInterest other) {
		BigDecimal dx = x.subtract(other.x);
		BigDecimal dy = y.subtract(other.y);
		return dx.multiply(dx).add(dy.multiply(dy));
	}
}