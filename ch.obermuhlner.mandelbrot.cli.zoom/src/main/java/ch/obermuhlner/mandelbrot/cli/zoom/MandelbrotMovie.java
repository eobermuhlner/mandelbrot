package ch.obermuhlner.mandelbrot.cli.zoom;

import ch.obermuhlner.mandelbrot.palette.MixPalette;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.palette.PaletteFactory;
import ch.obermuhlner.mandelbrot.palette.PaletteType;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

public class MandelbrotMovie {

    private static InterpolatorFactory<BigDecimal> LINEAR = (BigDecimal start, BigDecimal end) -> {
        return new FunctionInterpolator(start, end, x -> x);
    };

    private static InterpolatorFactory<BigDecimal> SMOOTH = (BigDecimal start, BigDecimal end) -> {
        return new FunctionInterpolator(start, end, x -> x * x * (3 - 2 * x));
    };

    private static InterpolatorFactory<BigDecimal> SMOOTHER = (BigDecimal start, BigDecimal end) -> {
        return new FunctionInterpolator(start, end, x -> x * x * x * (x * (x * 6 - 15) + 10));
    };

    public static void main(String[] args) {
        PaletteFactory paletteFactory = new PaletteFactory();

        List<MovieStep> movieSteps = new ArrayList<>();
        movieSteps.add(new MovieStep(
                new BigDecimal("0"),
                new BigDecimal("0"),
                new BigDecimal("0"),
                paletteFactory.createPalette(PaletteType.RandomColor, 14, 20)));
        // thorns
        movieSteps.add(new MovieStep(
                new BigDecimal("0.017919288259557892593847458731170858208746794667084869140010189701941671977885175491969822815680324832650288118287542225754354320968861805008056865043840568408674639001904242945377670838006845358428388295630356802224461622401647761424530072"),
                new BigDecimal("1.01176097531987061853463090956462940839175503684365364035585094020187264122252703179366559338808085425394834006826628793779676461877510495451535804537480898432580908583825399893338707858055917113599638919828012253992937320289924731178786342"),
                new BigDecimal("1"),
                paletteFactory.createPalette(PaletteType.RandomColor, 14, 20)));
        movieSteps.add(new MovieStep(
                new BigDecimal("0.017919288259557892593847458731170858208746794667084869140010189701941671977885175491969822815680324832650288118287542225754354320968861805008056865043840568408674639001904242945377670838006845358428388295630356802224461622401647761424530072"),
                new BigDecimal("1.01176097531987061853463090956462940839175503684365364035585094020187264122252703179366559338808085425394834006826628793779676461877510495451535804537480898432580908583825399893338707858055917113599638919828012253992937320289924731178786342"),
                new BigDecimal("11"),
                paletteFactory.createPalette(PaletteType.Fire, 1, 60)));
        movieSteps.add(new MovieStep(
                new BigDecimal("0.017919288259557892593847458731170858208746794667084869140010189701941671977885175491969822815680324832650288118287542225754354320968861805008056865043840568408674639001904242945377670838006845358428388295630356802224461622401647761424530072"),
                new BigDecimal("1.01176097531987061853463090956462940839175503684365364035585094020187264122252703179366559338808085425394834006826628793779676461877510495451535804537480898432580908583825399893338707858055917113599638919828012253992937320289924731178786342"),
                new BigDecimal("1"),
                paletteFactory.createPalette(PaletteType.Fire, 1, 60)));
        // snail shell
        movieSteps.add(new MovieStep(
                new BigDecimal("1.749721929742338571328512183204793465117897644259904770681747353482121708665972660839800936317633296441980469665826685985285388"),
                new BigDecimal("-0.000029016647753686084545360932647113026525960648184743451989371745927172996759411354785502498950354939680922076169129062986700"),
                new BigDecimal("1"),
                paletteFactory.createPalette(PaletteType.Water, 1, 20)));
        movieSteps.add(new MovieStep(
                new BigDecimal("1.749721929742338571328512183204793465117897644259904770681747353482121708665972660839800936317633296441980469665826685985285388"),
                new BigDecimal("-0.000029016647753686084545360932647113026525960648184743451989371745927172996759411354785502498950354939680922076169129062986700"),
                new BigDecimal("11.0"),
                paletteFactory.createPalette(PaletteType.Water, 1, 150)));
        movieSteps.add(new MovieStep(
                new BigDecimal("1.749721929742338571328512183204793465117897644259904770681747353482121708665972660839800936317633296441980469665826685985285388"),
                new BigDecimal("-0.000029016647753686084545360932647113026525960648184743451989371745927172996759411354785502498950354939680922076169129062986700"),
                new BigDecimal("11.3"),
                paletteFactory.createPalette(PaletteType.RandomColor, 1, 5)));
        movieSteps.add(new MovieStep(
                new BigDecimal("1.749721929742338571328512183204793465117897644259904770681747353482121708665972660839800936317633296441980469665826685985285388"),
                new BigDecimal("-0.000029016647753686084545360932647113026525960648184743451989371745927172996759411354785502498950354939680922076169129062986700"),
                new BigDecimal("1.0"),
                paletteFactory.createPalette(PaletteType.RandomColor, 1, 5)));
        movieSteps.add(new MovieStep(
                new BigDecimal("0"),
                new BigDecimal("0"),
                new BigDecimal("0"),
                paletteFactory.createPalette(PaletteType.RandomColor, 14, 20)));

        createMovie(movieSteps, 100);
    }

    private static void createMovie(List<MovieStep> movieSteps, int frames) {
        int imageIndex = 0;
        MovieStep lastStep = null;
        for (MovieStep currentStep: movieSteps) {
            if (lastStep != null) {
                Interpolator<BigDecimal> xInterpolator = currentStep.positionInterpolatorFactory.create(lastStep.x, currentStep.x);
                Interpolator<BigDecimal> yInterpolator = currentStep.positionInterpolatorFactory.create(lastStep.y, currentStep.y);
                Interpolator<BigDecimal> zoomInterpolator = currentStep.zoomInterpolatorFactory.create(lastStep.zoom, currentStep.zoom);

                Path outDir = Paths.get("images", "zoom");
                outDir.toFile().mkdirs();

                for (int frame = 0; frame < frames; frame++) {
                    double value = ((double)frame) / frames;
                    BigDecimal x = xInterpolator.interpolate(value);
                    BigDecimal y = yInterpolator.interpolate(value);
                    BigDecimal zoom = zoomInterpolator.interpolate(value);

                    System.out.println("IMAGE " + imageIndex + " x=" + x + " y=" + y + " zoom=" + zoom);
                    Palette palette = new MixPalette(lastStep.palette, currentStep.palette, value);
                    renderImage(outDir, imageIndex, x, y, zoom, palette);
                    imageIndex++;
                }
            }
            lastStep = currentStep;
        }
    }

    private static void renderImage(Path outDir, int imageIndex, BigDecimal x, BigDecimal y, BigDecimal zoom, Palette palette) {
        String filename = String.format("mandelbrot%04d.png", imageIndex);
        File file = outDir.resolve(filename).toFile();

        BigDecimal zoomStart = BigDecimal.valueOf(5);
        int maxIterationsConst = 1000;
        int maxIterationsLinear = 1000;
        double colorOffset = 0.0;

        MandelbrotZoom.renderImage(file, x, y, zoomStart, zoom, maxIterationsConst, maxIterationsLinear, palette, colorOffset);
    }

    public interface Interpolator<T> {
        T interpolate(double x);
    }

    public static class FunctionInterpolator implements Interpolator<BigDecimal> {
        private final BigDecimal start;
        private DoubleFunction<Double> function;
        private final BigDecimal range;

        public FunctionInterpolator(BigDecimal start, BigDecimal end, DoubleFunction<Double> function) {
            this.start = start;
            this.range = end.subtract(start);
            this.function = function;
        }
        @Override
        public BigDecimal interpolate(double x) {
            return range.multiply(BigDecimal.valueOf(function.apply(x))).add(start);
        }
    }

    public interface InterpolatorFactory<T> {
        Interpolator<T> create(T start, T end);
    }

    private static class MovieStep {
        public final BigDecimal x;
        public final BigDecimal y;
        public final BigDecimal zoom;
        public final InterpolatorFactory<BigDecimal> positionInterpolatorFactory;
        public final InterpolatorFactory<BigDecimal> zoomInterpolatorFactory;
        private Palette palette;

        public MovieStep(BigDecimal x, BigDecimal y, BigDecimal zoom, Palette palette) {
            this(x, y, zoom, SMOOTHER, SMOOTHER, palette);
        }

        public MovieStep(BigDecimal x, BigDecimal y, BigDecimal zoom,
                         InterpolatorFactory<BigDecimal> positionInterpolatorFactory,
                         InterpolatorFactory<BigDecimal> zoomInterpolatorFactory,
                         Palette palette) {
            this.x = x;
            this.y = y;
            this.zoom = zoom;
            this.positionInterpolatorFactory = positionInterpolatorFactory;
            this.zoomInterpolatorFactory = zoomInterpolatorFactory;
            this.palette = palette;
        }
    }
}
