package ch.obermuhlner.mandelbrot.javafx;

import javafx.scene.paint.Color;

public interface Palette {
	Color getColor(int iterations);
}
