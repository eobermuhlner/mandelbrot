package ch.obermuhlner.mandelbrot.imagegen;

import java.awt.Color;

public interface Palette {
	Color getColor(int iterations);
}
