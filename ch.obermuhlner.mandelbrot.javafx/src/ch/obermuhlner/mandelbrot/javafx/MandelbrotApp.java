package ch.obermuhlner.mandelbrot.javafx;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/*
 * Nice points:
 * 0.56255802552 0.65043192728
 * -0.04729622199 0.66103581600
 * 0.56268187195 0.64225590163
 */
public class MandelbrotApp extends Application {

	private static final double KEY_TRANSLATE_FACTOR = 0.1;
	private static final double KEY_ZOOM_STEP = 0.1;
	private static final double SCROLL_ZOOM_STEP = 0.1;

	private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("##0");
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("##0.000000");
	
	private static final StringConverter<BigDecimal> BIGDECIMAL_STRING_CONVERTER = new StringConverter<BigDecimal>() {
		@Override
		public String toString(BigDecimal object) {
			return object.toString();
		}

		@Override
		public BigDecimal fromString(String string) {
			try {
				return new BigDecimal(string);
			} catch (NumberFormatException ex) {
				return BigDecimal.ZERO;
			}
		}
	};

	private class BackgroundRenderer extends Thread {
		private volatile boolean backgroundRunning;
		private DrawRequest nextDrawRequest;
		
		private synchronized  void triggerDraw(DrawRequest drawRequest) {
			nextDrawRequest = drawRequest;
			notifyAll();
		}
		
		private synchronized DrawRequest getNextDrawRequest() {
			DrawRequest result = nextDrawRequest;
			nextDrawRequest = null;
			return result;
		}
		
		public synchronized void stopRunning() {
			backgroundRunning = false;
			notifyAll();
		}

		public void run() {
			backgroundRunning = true;

			while (backgroundRunning) {
				DrawRequest currentDrawRequest = getNextDrawRequest();
				if (currentDrawRequest != null) {
					BlockRenderInfo[] progressiveRenderInfos = currentDrawRequest.getProgressiveRenderInfo();
					
					int block = 0;
					while (block < progressiveRenderInfos.length) {
						BlockRenderInfo blockRenderInfo = progressiveRenderInfos[block];
						calculateMandelbrot(currentDrawRequest, blockRenderInfo.blockSize, blockRenderInfo.pixelOffsetX, blockRenderInfo.pixelOffsetY, blockRenderInfo.pixelSize);
						Platform.runLater(() -> {
							drawMandelbrot();
						});
	
						DrawRequest anotherDrawRequest = getNextDrawRequest();
						if (anotherDrawRequest == null) {
							block++;
						} else {
							currentDrawRequest = anotherDrawRequest;
							progressiveRenderInfos = currentDrawRequest.getProgressiveRenderInfo();
							block = 0;
						}
					}
				}
				
				try {
					synchronized(this) {
						wait();
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	private ObjectProperty<BigDecimal> xCenterProperty = new SimpleObjectProperty<BigDecimal>(BigDecimal.ZERO);
	private ObjectProperty<BigDecimal> yCenterProperty = new SimpleObjectProperty<BigDecimal>(BigDecimal.ZERO);
	private DoubleProperty zoomProperty = new SimpleDoubleProperty(0.0);
	private IntegerProperty paletteSeedProperty = new SimpleIntegerProperty(14);
	private IntegerProperty paletteStepProperty = new SimpleIntegerProperty(20);
	
	private BooleanProperty crosshairProperty = new SimpleBooleanProperty(true); 

	private Palette palette;

	private Canvas mandelbrotCanvas;
	private WritableImage image = new WritableImage(800, 800);
	
	private BackgroundRenderer backgroundRenderer;

	@Override
	public void start(Stage primaryStage) throws Exception {
		backgroundRenderer = new BackgroundRenderer();
		backgroundRenderer.start();

		Group root = new Group();
		Scene scene = new Scene(root);
		
		BorderPane borderPane = new BorderPane();
		root.getChildren().add(borderPane);
		
		Node toolbar = createToolbar();
		borderPane.setTop(toolbar);

		Node editor = createEditor();
		borderPane.setRight(editor);

		mandelbrotCanvas = createMandelbrotCanvas();
		borderPane.setCenter(mandelbrotCanvas);
		
		primaryStage.setScene(scene);
		primaryStage.show();
		
		mandelbrotCanvas.requestFocus();
	}

	@Override
	public void stop() throws Exception {
		backgroundRenderer.stopRunning();
		
		super.stop();
	}
	
	private Canvas createMandelbrotCanvas() {
		double height = 800;
		double width = 800;
		
		Canvas canvas = new Canvas(width, height);
		canvas.setFocusTraversable(true);
		
		setupCanvasEventHandlers(canvas);

		updatePalette(canvas);

		return canvas;
	}

	private Node createToolbar() {
		HBox box = new HBox(2);
		
		ToggleButton crosshairToggleButton = new ToggleButton("Crosshair");
		box.getChildren().add(crosshairToggleButton);
		Bindings.bindBidirectional(crosshairToggleButton.selectedProperty(), crosshairProperty);
		
		return box;
	}
	
	private Node createEditor() {
		HBox box = new HBox();
		Slider zoomSlider = new Slider(0.0, 100.0, 0.0);
		zoomSlider.setOrientation(Orientation.VERTICAL);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setMajorTickUnit(1.0f);
        box.getChildren().add(zoomSlider);
        Bindings.bindBidirectional(zoomProperty, zoomSlider.valueProperty());

        GridPane gridPane = new GridPane();
        box.getChildren().add(gridPane);
        gridPane.setHgap(4);
        gridPane.setVgap(4);
		
		int rowIndex = 0;
		
		gridPane.add(new Label("X:"), 0, rowIndex);
		TextField xCenterTextField = new TextField();
		gridPane.add(xCenterTextField, 1, rowIndex);
		Bindings.bindBidirectional(xCenterTextField.textProperty(), xCenterProperty, BIGDECIMAL_STRING_CONVERTER);
		rowIndex++;
		
		gridPane.add(new Label("Y:"), 0, rowIndex);
		TextField yCenterTextField = new TextField();
		gridPane.add(yCenterTextField, 1, rowIndex);
		Bindings.bindBidirectional(yCenterTextField.textProperty(), yCenterProperty, BIGDECIMAL_STRING_CONVERTER);
		rowIndex++;
		
		gridPane.add(new Label("Zoom:"), 0, rowIndex);
		TextField zoomTextField = new TextField();
		gridPane.add(zoomTextField, 1, rowIndex);
		Bindings.bindBidirectional(zoomTextField.textProperty(), zoomProperty, DOUBLE_FORMAT);
		rowIndex++;

		gridPane.add(new Label("Color Scheme:"), 0, rowIndex);
		Spinner<Integer> paletteSeedSpinner = new Spinner<Integer>(0, 999, paletteSeedProperty.get());
		gridPane.add(paletteSeedSpinner, 1, rowIndex);
		paletteSeedSpinner.setEditable(true);
		paletteSeedSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
			paletteSeedProperty.set(newValue);
		});
		rowIndex++;

		gridPane.add(new Label("Color Step:"), 0, rowIndex);
		TextField paletteStepTextField = new TextField();
		gridPane.add(paletteStepTextField, 1, rowIndex);
		Bindings.bindBidirectional(paletteStepTextField.textProperty(), paletteStepProperty, INTEGER_FORMAT);
		rowIndex++;
		
		return box;
	}

	double lastMouseDragX;
	double lastMouseDragY;
	private void setupCanvasEventHandlers(Canvas canvas) {
		canvas.setOnMousePressed(event -> {
			lastMouseDragX = event.getX();
			lastMouseDragY = event.getY();
		});
		canvas.setOnMouseDragged(event -> {
			double deltaX = event.getX() - lastMouseDragX;
			double deltaY = event.getY() - lastMouseDragY;
			lastMouseDragX = event.getX();
			lastMouseDragY = event.getY();
			
			translateMandelbrot(canvas, deltaX, deltaY);
		});
		canvas.setOnMouseReleased(event -> {
			double deltaX = event.getX() - lastMouseDragX;
			double deltaY = event.getY() - lastMouseDragY;
			lastMouseDragX = event.getX();
			lastMouseDragY = event.getY();

			translateMandelbrot(canvas, deltaX, deltaY);
		});
	
		canvas.setOnZoom(event -> {
			double zoomStep = event.getZoomFactor() - 1.0;
			zoomMandelbrot(canvas, zoomStep);
		});
		canvas.setOnZoomFinished(event -> {
			calculateAndDrawMandelbrot(canvas);
		});
		
		canvas.setOnScroll(event -> {
			if (!event.isDirect()) {
				double deltaY = event.getDeltaY();
				
				zoomScrollMandelbrot(canvas, deltaY);
			}
		});
		
		canvas.setOnKeyPressed(event -> {
			switch (event.getCode()) {
			case UP:
				zoomMandelbrot(canvas, KEY_ZOOM_STEP);
				break;
			case DOWN:
				zoomMandelbrot(canvas, -KEY_ZOOM_STEP);
				break;
			case W:
				translateMandelbrot(canvas, 0.0, canvas.getHeight() * KEY_TRANSLATE_FACTOR);
				break;
			case A:
				translateMandelbrot(canvas, canvas.getWidth() * KEY_TRANSLATE_FACTOR, 0.0);
				break;
			case S:
				translateMandelbrot(canvas, 0.0, -canvas.getHeight() * KEY_TRANSLATE_FACTOR);
				break;
			case D:
				translateMandelbrot(canvas, -canvas.getWidth() * KEY_TRANSLATE_FACTOR, 0.0);
				break;
			default:
			}
			event.consume();
		});

		paletteSeedProperty.addListener((observable, oldValue, newValue) -> {
			updatePalette(canvas);
		});
		paletteStepProperty.addListener((observable, oldValue, newValue) -> {
			updatePalette(canvas);
		});

		xCenterProperty.addListener((observable, oldValue, newValue) -> {
			calculateAndDrawMandelbrot(canvas);
		});
		yCenterProperty.addListener((observable, oldValue, newValue) -> {
			calculateAndDrawMandelbrot(canvas);
		});
		zoomProperty.addListener((observable, oldValue, newValue) -> {
			calculateAndDrawMandelbrot(canvas);
		});
		
		crosshairProperty.addListener((observable, oldValue, newValue) -> {
			drawMandelbrot();
		});
	}
	
	private void updatePalette(Canvas canvas) {
		int seed = paletteSeedProperty.get();
		int steps = paletteStepProperty.get();
		if (steps <= 0) {
			steps = 10;
		}
		palette = new CachingPalette(new RandomPalette(seed, steps));
		calculateAndDrawMandelbrot(canvas);
	}

	private void translateMandelbrot(Canvas canvas, double deltaPixelX, double deltaPixelY) {
		BigDecimal pixelWidth = BigDecimal.valueOf(canvas.getWidth());
		BigDecimal pixelHeight = BigDecimal.valueOf(canvas.getHeight());

		
		BigDecimal radius = MandelbrotMath.getRadius(zoomProperty.get());
		BigDecimal deltaX = BigDecimal.valueOf(deltaPixelX).divide(pixelWidth, MathContext.DECIMAL128).multiply(radius);
		BigDecimal deltaY = BigDecimal.valueOf(deltaPixelY).divide(pixelHeight, MathContext.DECIMAL128).multiply(radius);
		
		xCenterProperty.set(xCenterProperty.get().add(deltaX));
		yCenterProperty.set(yCenterProperty.get().add(deltaY));
		
		calculateAndDrawMandelbrot(canvas);
	}

	private void zoomScrollMandelbrot(Canvas canvas, double deltaPixelY) {
		double pixelHeight = canvas.getHeight();
	
		double deltaZ = deltaPixelY / pixelHeight * 2.0;

		System.out.println("SCROLLSTEP " + deltaZ);
		zoomProperty.set(zoomProperty.get() + deltaZ * SCROLL_ZOOM_STEP); // FIXME
		//radiusProperty.set(radiusProperty.get().multiply(BigDecimal.valueOf(1.0 + deltaZ)));

		calculateAndDrawMandelbrot(canvas);
	}

	private void zoomMandelbrot(Canvas canvas, double zoomStep) {
		zoomProperty.set(zoomProperty.get() + zoomStep);

		calculateAndDrawMandelbrot(canvas);
	}
	
	private void calculateAndDrawMandelbrot(Canvas canvas) {
		backgroundRenderer.triggerDraw(new DrawRequest(xCenterProperty.get(), yCenterProperty.get(), zoomProperty.get()));
	}		
	
	private void drawMandelbrot() {
		GraphicsContext gc = mandelbrotCanvas.getGraphicsContext2D();
		gc.drawImage(image, 0, 0);

		if (crosshairProperty.get()) {
			gc.setStroke(Color.WHITE);
			gc.strokeLine(mandelbrotCanvas.getWidth() / 2, 0, mandelbrotCanvas.getWidth() / 2, mandelbrotCanvas.getHeight());
			gc.strokeLine(0, mandelbrotCanvas.getHeight() / 2, mandelbrotCanvas.getWidth(), mandelbrotCanvas.getHeight() / 2);
		}
	}

	private void calculateMandelbrot(DrawRequest drawRequest, int blockSize, int blockPixelOffsetX, int blockPixelOffsetY, int pixelSize) {
		if (drawRequest.isInsideDoublePrecision()) {
			calculateMandelbrotDouble(drawRequest, blockSize, blockPixelOffsetX, blockPixelOffsetY, pixelSize);
		} else {
			calculateMandelbrotBigDecimal(drawRequest, blockSize, blockPixelOffsetX, blockPixelOffsetY, pixelSize);
		}
	}
	
	private void calculateMandelbrotDouble(DrawRequest drawRequest, int blockSize, int blockPixelOffsetX, int blockPixelOffsetY, int pixelSize) {
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double pixelWidth = image.getWidth();
		double pixelHeight = image.getHeight();

		double xRadius = drawRequest.getRadius().doubleValue();
		double yRadius = xRadius;
		double xCenter = drawRequest.x.doubleValue();
		double yCenter = drawRequest.y.doubleValue();
		int maxIteration = drawRequest.getMaxIteration();
		
		double pixelStepX = xRadius*2 / pixelWidth;
		double pixelStepY = yRadius*2 / pixelHeight;
		double blockStepX = pixelStepX * blockSize;
		double blockStepY = pixelStepY * blockSize;
		double x0 = pixelStepX * blockPixelOffsetX - xCenter - xRadius;
		
		for (int pixelX = blockPixelOffsetX; pixelX < pixelWidth; pixelX+=blockSize) {
			double y0 = pixelStepY * blockPixelOffsetY - yCenter - yRadius; 
			for (int pixelY = blockPixelOffsetY; pixelY < pixelHeight; pixelY+=blockSize) {
				double x = 0;
				double y = 0;
				int iteration = 0;
				double xx = x*x;
				double yy = y*y;
				while (xx + yy < 2*2 && iteration < maxIteration) {
					y = 2*x*y + y0;
					x = xx - yy + x0;
					iteration++;
					
					xx = x*x;
					yy = y*y;
				}

				Color color = iteration == maxIteration ? Color.BLACK : palette.getColor(iteration);
				for (int pixelOffsetX = 0; pixelOffsetX < pixelSize; pixelOffsetX++) {
					for (int pixelOffsetY = 0; pixelOffsetY < pixelSize; pixelOffsetY++) {
						int px = pixelX + pixelOffsetX;
						int py = pixelY + pixelOffsetY;
						if (px < pixelWidth && py < pixelHeight) {
							pixelWriter.setColor(px, py, color);
						}
					}
				}
				y0 += blockStepY;
			}
			x0 += blockStepX;
		}
	}

	private void calculateMandelbrotBigDecimal(DrawRequest drawRequest, int blockSize, int blockPixelOffsetX, int blockPixelOffsetY, int pixelSize) {
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double pixelWidth = image.getWidth();
		double pixelHeight = image.getHeight();
		
		MathContext mc = MathContext.DECIMAL128;

		BigDecimal xRadius = drawRequest.getRadius();
		BigDecimal yRadius = xRadius;
		BigDecimal xCenter = drawRequest.x;
		BigDecimal yCenter = drawRequest.y;
		int maxIteration = drawRequest.getMaxIteration();
		
		BigDecimal two = new BigDecimal(2);
		BigDecimal four = new BigDecimal(4);
		BigDecimal pixelStepX = xRadius.multiply(two, mc).divide(BigDecimal.valueOf(pixelWidth), mc);
		BigDecimal pixelStepY = yRadius.multiply(two, mc).divide(BigDecimal.valueOf(pixelHeight), mc);
		BigDecimal blockStepX = pixelStepX.multiply(new BigDecimal(blockSize), mc);
		BigDecimal blockStepY = pixelStepY.multiply(new BigDecimal(blockSize), mc);
		BigDecimal x0 = pixelStepX.multiply(new BigDecimal(blockPixelOffsetX), mc).subtract(xCenter, mc).subtract(xRadius, mc);
		
		for (int pixelX = blockPixelOffsetX; pixelX < pixelWidth; pixelX+=blockSize) {
			BigDecimal y0 = pixelStepY.multiply(new BigDecimal(blockPixelOffsetY), mc).subtract(yCenter, mc).subtract(yRadius, mc);
			for (int pixelY = blockPixelOffsetY; pixelY < pixelHeight; pixelY+=blockSize) {
				BigDecimal x = BigDecimal.ZERO;
				BigDecimal y = BigDecimal.ZERO;
				int iteration = 0;
				BigDecimal xx = x;
				BigDecimal yy = y;
				while (xx.add(yy, mc).compareTo(four) < 0 && iteration < maxIteration) {
					y = two.multiply(x, mc).multiply(y, mc).add(y0, mc);
					x = xx.subtract(yy, mc).add(x0, mc);
					iteration++;
					
					xx = x.multiply(x, mc);
					yy = y.multiply(y, mc);
				}

				Color color = iteration == maxIteration ? Color.BLACK : palette.getColor(iteration);
				for (int pixelOffsetX = 0; pixelOffsetX < pixelSize; pixelOffsetX++) {
					for (int pixelOffsetY = 0; pixelOffsetY < pixelSize; pixelOffsetY++) {
						int px = pixelX + pixelOffsetX;
						int py = pixelY + pixelOffsetY;
						if (px < pixelWidth && py < pixelHeight) {
							pixelWriter.setColor(px, py, color);
						}
					}
				}
				y0 = y0.add(blockStepY, mc);
			}
			x0 = x0.add(blockStepX, mc);
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

}
