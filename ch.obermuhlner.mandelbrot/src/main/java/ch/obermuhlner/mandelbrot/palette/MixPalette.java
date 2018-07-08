package ch.obermuhlner.mandelbrot.palette;

public class MixPalette implements Palette {
    private final Palette startPalette;
    private final Palette endPalette;
    private final double mix;

    public MixPalette(Palette startPalette, Palette endPalette, double mix) {
        this.startPalette = startPalette;
        this.endPalette = endPalette;
        this.mix = mix;
    }

    @Override
    public Color getColor(int iterations) {
        Color startColor = startPalette.getColor(iterations);
        Color endColor = endPalette.getColor(iterations);
        return startColor.interpolate(endColor, mix);
    }
}
