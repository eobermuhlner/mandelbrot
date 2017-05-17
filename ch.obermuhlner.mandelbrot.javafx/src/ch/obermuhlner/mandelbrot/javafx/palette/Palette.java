package ch.obermuhlner.mandelbrot.javafx.palette;

import javafx.scene.paint.Color;

public interface Palette {
	Color getColor(int iterations);
}
