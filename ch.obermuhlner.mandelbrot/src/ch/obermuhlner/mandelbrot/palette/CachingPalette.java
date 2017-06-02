package ch.obermuhlner.mandelbrot.palette;

import java.util.HashMap;
import java.util.Map;


public class CachingPalette implements Palette {

	private final Palette palette;
	
	private final Map<Integer, Color> cache = new HashMap<>();

	public CachingPalette(Palette palette) {
		this.palette = palette;
	}
	
	@Override
	public Color getColor(int iterations) {
		Color color = cache.get(iterations);
		
		if (color == null) {
			color = palette.getColor(iterations);
			cache.put(iterations, color);
		}
		
		return color;
	}
}
