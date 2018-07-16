package ch.obermuhlner.mandelbrot.movie;

import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.math.big.BigDecimalMath;

import java.math.BigDecimal;
import java.math.MathContext;

public class MovieStep {
    public final BigDecimal x;
    public final BigDecimal y;
    public final BigDecimal zoom;
    public final Palette palette;

    public MovieStep(BigDecimal x, BigDecimal y, BigDecimal zoom, Palette palette) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.palette = palette;
    }

    public BigDecimal distanceSquare(MovieStep other) {
        BigDecimal dx = x.subtract(other.x);
        BigDecimal dy = y.subtract(other.y);

        return dx.multiply(dx).add(dy.multiply(dy));
    }

    public BigDecimal distance(MovieStep other, MathContext mc) {
        return BigDecimalMath.sqrt(distanceSquare(other), mc);
    }
}
