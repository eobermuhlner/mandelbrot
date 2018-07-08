package ch.obermuhlner.mandelbrot.cli.zoom;

import ch.obermuhlner.mandelbrot.palette.MixPalette;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.palette.PaletteFactory;
import ch.obermuhlner.mandelbrot.poi.PointOfInterest;
import ch.obermuhlner.mandelbrot.poi.StandardPointsOfInterest;

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


    private final static double EASE_POWER = 7; // must be odd number
    private static InterpolatorFactory<BigDecimal> EASE_IN = (BigDecimal start, BigDecimal end) -> {
        return new FunctionInterpolator(start, end, x -> Math.pow(x, EASE_POWER));
    };

    private static InterpolatorFactory<BigDecimal> EASE_OUT = (BigDecimal start, BigDecimal end) -> {
        return new FunctionInterpolator(start, end, x -> Math.pow(x - 1, EASE_POWER) + 1);
    };


    public static void main(String[] args) {
        List<MovieStep> movieSteps = new ArrayList<>();
        List<PointOfInterest> pointsOfInterest = travellingSalesman(StandardPointsOfInterest.POINTS_OF_INTEREST);
        for (PointOfInterest poi : pointsOfInterest) {
            movieSteps.add(toMovieStep(poi));
        }
        movieSteps.add(toMovieStep(pointsOfInterest.get(0)));

        createMovie(movieSteps, 100);
    }

    private static List<PointOfInterest> travellingSalesman(PointOfInterest[] pointsOfInterest) {
        return travellingSalesman(pointsOfInterest);
    }

    private static MovieStep toMovieStep(PointOfInterest poi) {
        PaletteFactory paletteFactory = new PaletteFactory();
        Palette palette = paletteFactory.createPalette(poi.paletteType, poi.paletteSeed, poi.paletteStep);
        return new MovieStep(poi.x, poi.y, BigDecimal.valueOf(poi.zoom), palette);
    }

    private static void createMovie(List<MovieStep> movieSteps, int frames) {
        BigDecimal intermediateZoom = BigDecimal.valueOf(0.5);

        int imageIndex = 0;
        MovieStep lastStep = null;
        for (MovieStep currentStep: movieSteps) {
            if (lastStep != null) {
                if (needIntermediateStep(lastStep, currentStep)) {
                    MovieStep half1Step = new MovieStep(lastStep.x, lastStep.y, intermediateZoom, lastStep.palette);
                    imageIndex = interpolateBetweenMovieSteps(frames, imageIndex, lastStep, half1Step, SMOOTH, SMOOTH); // easeInOrOut(lastStep.zoom, halfStepZoom)

                    MovieStep half2Step = new MovieStep(currentStep.x, currentStep.y, intermediateZoom, lastStep.palette);
                    imageIndex = interpolateBetweenMovieSteps(frames, imageIndex, half1Step, half2Step, SMOOTH, SMOOTH);

                    imageIndex = interpolateBetweenMovieSteps(frames, imageIndex, half2Step, currentStep, SMOOTH, SMOOTH);
                } else {
                    imageIndex = interpolateBetweenMovieSteps(frames, imageIndex, lastStep, currentStep, SMOOTH, SMOOTH);
                }
            }
            lastStep = currentStep;
        }
    }

    private static boolean needIntermediateStep(MovieStep firstStep, MovieStep secondStep) {
        return true;
    }

    private static InterpolatorFactory<BigDecimal> easeInOrOut(BigDecimal startZoom, BigDecimal endZoom) {
        if (startZoom.compareTo(endZoom) <= 0) {
            return EASE_OUT;
        } else {
            return EASE_IN;
        }
    }

    private static int interpolateBetweenMovieSteps(
            int frames,
            int imageIndex,
            MovieStep lastStep,
            MovieStep currentStep,
            InterpolatorFactory<BigDecimal> positionInterpolatorFactory,
            InterpolatorFactory<BigDecimal> zoomInterpolatorFactory) {
        Interpolator<BigDecimal> xInterpolator = positionInterpolatorFactory.create(lastStep.x, currentStep.x);
        Interpolator<BigDecimal> yInterpolator = positionInterpolatorFactory.create(lastStep.y, currentStep.y);
        Interpolator<BigDecimal> zoomInterpolator = zoomInterpolatorFactory.create(lastStep.zoom, currentStep.zoom);

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
        return imageIndex;
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
        private final BigDecimal end;
        private DoubleFunction<Double> function;
        private final BigDecimal range;

        public FunctionInterpolator(BigDecimal start, BigDecimal end, DoubleFunction<Double> function) {
            this.start = start;
            this.end = end;
            this.range = end.subtract(start);
            this.function = function;
        }
        @Override
        public BigDecimal interpolate(double x) {
            if (x <= 0.0) {
                return start;
            }
            if (x >= 1.0) {
                return end;
            }
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
        public final Palette palette;

        public MovieStep(BigDecimal x, BigDecimal y, BigDecimal zoom, Palette palette) {
            this.x = x;
            this.y = y;
            this.zoom = zoom;
            this.palette = palette;
        }
    }
}
