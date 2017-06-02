package ch.obermuhlner.mandelbrot.javafx;

import ch.obermuhlner.mandelbrot.palette.Color;

public class ColorUtil {

	public static javafx.scene.paint.Color toJavafxColor(Color color) {
		if (color == null) {
			return javafx.scene.paint.Color.TRANSPARENT;
		}
		return new javafx.scene.paint.Color(color.getRed(), color.getGreen(), color.getBlue(), 1);
	}
}
