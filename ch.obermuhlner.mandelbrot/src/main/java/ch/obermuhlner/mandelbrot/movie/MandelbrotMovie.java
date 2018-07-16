package ch.obermuhlner.mandelbrot.movie;

import ch.obermuhlner.mandelbrot.javafx.DummyProgress;
import ch.obermuhlner.mandelbrot.javafx.Progress;
import ch.obermuhlner.mandelbrot.palette.MixPalette;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.palette.PaletteFactory;
import ch.obermuhlner.mandelbrot.poi.PointOfInterest;
import ch.obermuhlner.mandelbrot.poi.StandardPointsOfInterest;
import ch.obermuhlner.mandelbrot.render.AutoPrecisionMandelbrotRenderer;
import ch.obermuhlner.mandelbrot.render.BufferedImageMandelbrotResult;
import ch.obermuhlner.mandelbrot.render.MandelbrotRenderer;
import ch.obermuhlner.mandelbrot.util.StopWatch;
import ch.obermuhlner.math.big.BigDecimalMath;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final double secondsPerTranslate;
    private final double secondsPerZoomLevel;
    private final double framesPerSecond;

    private final MandelbrotRenderer mandelbrotRenderer = new AutoPrecisionMandelbrotRenderer();

    public MandelbrotMovie() {
        this(1.0, 1.0, 24.0);
    }

    public MandelbrotMovie(double secondsPerTranslate, double secondsPerZoomLevel, double framesPerSecond) {
        this.secondsPerTranslate = secondsPerTranslate;
        this.secondsPerZoomLevel = secondsPerZoomLevel;
        this.framesPerSecond = framesPerSecond;
    }

    public void createMovie(Path directory, List<MovieStep> movieSteps) {
        double framesPerTranslate = secondsPerTranslate * framesPerSecond;
        double framePerZoomLevel = secondsPerZoomLevel * framesPerSecond;

        createMovie(directory, movieSteps, framesPerTranslate, framePerZoomLevel);
    }

    private void createMovie(Path directory, List<MovieStep> movieSteps, double framesPerTranslate, double framesPerZoomLevel ) {
        int imageIndex = 0;
        MovieStep lastStep = null;
        for (MovieStep currentStep: movieSteps) {
            if (lastStep != null) {
                int precision = (int) (Math.max(lastStep.zoom.doubleValue(), currentStep.zoom.doubleValue())* 1 + 10);
                MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
                BigDecimal stepDistance = lastStep.distance(currentStep, mc);
                BigDecimal intermediateZoom = BigDecimalMath.log10(stepDistance, MathContext.DECIMAL64).negate();
                System.out.println("STEP DISTANCE:     " + stepDistance);
                System.out.println("INTERMEDIATE ZOOM: " + intermediateZoom);

                boolean needFirstHalfStep = lastStep.zoom.compareTo(intermediateZoom) >= 0;
                boolean needSecondHalfStep = currentStep.zoom.compareTo(intermediateZoom) >= 0;

                if (needFirstHalfStep && needSecondHalfStep) {
                    System.out.println("HALF1 STEP: ");
                    MovieStep half1Step = new MovieStep(lastStep.x, lastStep.y, intermediateZoom, lastStep.palette);
                    imageIndex = interpolateBetweenMovieSteps(
                            directory,
                            toZoomFrames(lastStep, half1Step, framesPerZoomLevel),
                            imageIndex,
                            lastStep,
                            half1Step,
                            SMOOTH, SMOOTH); // easeInOrOut(lastStep.zoom, halfStepZoom)

                    System.out.println("HALF2 STEP: ");
                    MovieStep half2Step = new MovieStep(currentStep.x, currentStep.y, intermediateZoom, lastStep.palette);
                    imageIndex = interpolateBetweenMovieSteps(
                            directory,
                            toTranslateFrames(half1Step, half2Step, framesPerTranslate),
                            imageIndex,
                            half1Step,
                            half2Step,
                            SMOOTH, SMOOTH);

                    System.out.println("HALF3 STEP: ");
                    imageIndex = interpolateBetweenMovieSteps(
                            directory,
                            toZoomFrames(half2Step, currentStep, framesPerZoomLevel),
                            imageIndex,
                            half2Step,
                            currentStep,
                            SMOOTH, SMOOTH);
                } else if (needFirstHalfStep && !needSecondHalfStep) {
                    System.out.println("HALF1 STEP: ");
                    MovieStep half1Step = new MovieStep(lastStep.x, lastStep.y, intermediateZoom, lastStep.palette);
                    imageIndex = interpolateBetweenMovieSteps(
                            directory,
                            toZoomFrames(lastStep, half1Step, framesPerZoomLevel),
                            imageIndex,
                            lastStep,
                            half1Step,
                            SMOOTH, SMOOTH); // easeInOrOut(lastStep.zoom, halfStepZoom)

                    System.out.println("HALF2+HALF3 STEP: ");
                    imageIndex = interpolateBetweenMovieSteps(
                            directory,
                            toFrames(half1Step, currentStep, framesPerTranslate, framesPerZoomLevel),
                            imageIndex,
                            half1Step,
                            currentStep,
                            SMOOTH, SMOOTH);
                } else if (!needFirstHalfStep && needSecondHalfStep) {
                    System.out.println("HALF1+HALF2 STEP: ");
                    MovieStep half12Step = new MovieStep(currentStep.x, currentStep.y, intermediateZoom, lastStep.palette);
                    imageIndex = interpolateBetweenMovieSteps(
                            directory,
                            toFrames(lastStep, half12Step, framesPerTranslate, framesPerZoomLevel),
                            imageIndex,
                            lastStep,
                            half12Step,
                            SMOOTH, SMOOTH);

                    System.out.println("HALF3 STEP: ");
                    imageIndex = interpolateBetweenMovieSteps(
                            directory,
                            toZoomFrames(half12Step, currentStep, framesPerZoomLevel),
                            imageIndex,
                            half12Step,
                            currentStep,
                            SMOOTH, SMOOTH);
                } else if (!needFirstHalfStep && !needSecondHalfStep) {
                    System.out.println("HALF1+HALF2+HALF3 STEP: ");
                    imageIndex = interpolateBetweenMovieSteps(
                            directory,
                            toFrames(lastStep, currentStep, framesPerTranslate, framesPerZoomLevel),
                            imageIndex,
                            lastStep,
                            currentStep,
                            SMOOTH, SMOOTH);
                } else {
                    throw new IllegalStateException("All cases must be handled! Missing case: " + needFirstHalfStep + " " + needSecondHalfStep);
                }
            }
            lastStep = currentStep;
        }
    }

    private int toFrames(MovieStep firstStep, MovieStep secondStep, double framesPerTranslate, double framesPerZoomLevel) {
        return Math.max(
                toTranslateFrames(firstStep, secondStep, framesPerTranslate),
                toZoomFrames(firstStep, secondStep, framesPerZoomLevel));
    }

    private int toTranslateFrames(MovieStep firstStep, MovieStep secondStep, double framesPerTranslate) {
        return Math.max(1, (int) (framesPerTranslate + 0.5));
    }

    private int toZoomFrames(MovieStep firstStep, MovieStep secondStep, double framesPerZoomLevel) {
        double deltaZoom = firstStep.zoom.subtract(secondStep.zoom).abs().doubleValue();
        if (deltaZoom < 1.0) {
            deltaZoom = 0.1;
        }
        return (int) (deltaZoom * framesPerZoomLevel + 0.5);
    }

    private InterpolatorFactory<BigDecimal> easeInOrOut(BigDecimal startZoom, BigDecimal endZoom) {
        if (startZoom.compareTo(endZoom) <= 0) {
            return EASE_OUT;
        } else {
            return EASE_IN;
        }
    }

    private int interpolateBetweenMovieSteps(
            Path directory,
            int frames,
            int imageIndex,
            MovieStep lastStep,
            MovieStep currentStep,
            InterpolatorFactory<BigDecimal> positionInterpolatorFactory,
            InterpolatorFactory<BigDecimal> zoomInterpolatorFactory) {
        Interpolator<BigDecimal> xInterpolator = positionInterpolatorFactory.create(lastStep.x, currentStep.x);
        Interpolator<BigDecimal> yInterpolator = positionInterpolatorFactory.create(lastStep.y, currentStep.y);
        Interpolator<BigDecimal> zoomInterpolator = zoomInterpolatorFactory.create(lastStep.zoom, currentStep.zoom);

        directory.toFile().mkdirs();

        System.out.println("INTERPOLATE: " + frames + " frames");

        for (int frame = 0; frame < frames; frame++) {
            double value = ((double)frame) / frames;
            BigDecimal x = xInterpolator.interpolate(value);
            BigDecimal y = yInterpolator.interpolate(value);
            BigDecimal zoom = zoomInterpolator.interpolate(value);

            System.out.println("IMAGE " + imageIndex + " x=" + x + " y=" + y + " zoom=" + zoom);
            Palette palette = new MixPalette(lastStep.palette, currentStep.palette, value);
            renderImage(directory, imageIndex, x, y, zoom, palette);
            imageIndex++;
        }
        return imageIndex;
    }

    private void renderImage(Path outDir, int imageIndex, BigDecimal x, BigDecimal y, BigDecimal zoom, Palette palette) {
        String filename = String.format("mandelbrot%04d.png", imageIndex);
        File file = outDir.resolve(filename).toFile();

        BigDecimal zoomStart = BigDecimal.valueOf(5);
        int maxIterationsConst = 1000;
        int maxIterationsLinear = 1000;
        double colorOffset = 0.0;

        renderImage(file, x, y, zoomStart, zoom, maxIterationsConst, maxIterationsLinear, palette, colorOffset);
    }

    private void renderImage(File file, BigDecimal xCenter, BigDecimal yCenter, BigDecimal zoomStart, BigDecimal zoomPower, int maxIterationsConst, int maxIterationsLinear, Palette palette, double colorOffset) {
        if (file.exists()) {
            //System.out.println("Already calculated " + file.getName() + " with zoom " + zoomPower.toPlainString());
            return;
        }

        StopWatch stopWatch = new StopWatch();

        int precision = zoomPower.intValue() * 1 + 10;
        MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
        BigDecimal radius = zoomStart.multiply(BigDecimalMath.pow(BigDecimal.TEN, zoomPower.negate(), mc));
        int maxIterations = maxIterationsConst + zoomPower.intValue() * maxIterationsLinear;
        int imageWidth = 800;
        int imageHeight = 800;

        Progress progress = new DummyProgress();

        BufferedImageMandelbrotResult result = new BufferedImageMandelbrotResult(imageWidth, imageHeight, palette, colorOffset);
        mandelbrotRenderer.drawMandelbrot(
                result,
                xCenter,
                yCenter,
                radius,
                radius,
                precision,
                maxIterations,
                imageWidth,
                imageHeight,
                progress);

        try {
            System.out.println("Calculated " + file.getName() + " with zoom " + zoomPower.toPlainString() + " in " + stopWatch);
            ImageIO.write(result.getImage(), "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void main(String[] args) {
        List<MovieStep> movieSteps = new ArrayList<>();
        List<PointOfInterest> pointsOfInterest = travelingSalesman(StandardPointsOfInterest.POINTS_OF_INTEREST);
        for (PointOfInterest poi : pointsOfInterest) {
            movieSteps.add(toMovieStep(poi));
        }
        movieSteps.add(toMovieStep(pointsOfInterest.get(0)));
        Path directory = Paths.get("images", "zoom");

        MandelbrotMovie mandelbrotMovie = new MandelbrotMovie();
        mandelbrotMovie.createMovie(directory, movieSteps);
    }

    private static List<PointOfInterest> travelingSalesman(PointOfInterest[] pointsOfInterest) {
        List<PointOfInterest> remaining = new ArrayList<>(Arrays.asList(pointsOfInterest));
        List<PointOfInterest> result = new ArrayList<>();

        PointOfInterest current = remaining.remove(0);
        result.add(current);

        while (!remaining.isEmpty()) {
            PointOfInterest nearest = findNearest(remaining, current);
            remaining.remove(nearest);
            result.add(nearest);
        }

        return result;
    }

    private static PointOfInterest findNearest(List<PointOfInterest> points, PointOfInterest start) {
        PointOfInterest nearestPoint = null;
        BigDecimal nearestDistanceSquare = null;

        for (PointOfInterest point : points) {
            if (nearestPoint == null) {
                nearestPoint = point;
                nearestDistanceSquare = start.distanceSquare(point);
            } else {
                BigDecimal distanceSquare = start.distanceSquare(point);
                if (distanceSquare.compareTo(nearestDistanceSquare) < 0) {
                    nearestPoint = point;
                    nearestDistanceSquare = distanceSquare;
                }
            }
        }

        return nearestPoint;
    }

    private static MovieStep toMovieStep(PointOfInterest poi) {
        PaletteFactory paletteFactory = new PaletteFactory();
        Palette palette = paletteFactory.createPalette(poi.paletteType, poi.paletteSeed, poi.paletteStep);
        return new MovieStep(poi.x, poi.y, BigDecimal.valueOf(poi.zoom), palette);
    }

}
