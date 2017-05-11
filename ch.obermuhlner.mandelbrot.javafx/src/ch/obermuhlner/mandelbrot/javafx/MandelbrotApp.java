package ch.obermuhlner.mandelbrot.javafx;

import java.text.DecimalFormat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
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

	private static final int MAX_ITERATION = 1000;

	private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("##0");
	
	private static final StringConverter<Number> DOUBLE_STRING_CONVERTER = new StringConverter<Number>() {
		@Override
		public String toString(Number object) {
			return object.toString();
		}

		@Override
		public Double fromString(String string) {
			return Double.parseDouble(string);
		}
	};

	private static class BlockRenderInfo {
		public int blockSize;
		public int pixelOffsetX;
		public int pixelOffsetY;
		public int pixelSize;

		public BlockRenderInfo(int blockSize, int pixelOffsetX, int pixelOffsetY, int pixelSize) {
			this.blockSize = blockSize;
			this.pixelOffsetX = pixelOffsetX;
			this.pixelOffsetY = pixelOffsetY;
			this.pixelSize = pixelSize;
		}

		@Override
		public String toString() {
			return "BlockRenderInfo [blockSize=" + blockSize + ", pixelOffsetX=" + pixelOffsetX + ", pixelOffsetY=" + pixelOffsetY + ", pixelSize=" + pixelSize + "]";
		}
	}
	
	private static final BlockRenderInfo[] blockRenderInfos = {
			new BlockRenderInfo(4, 0, 0, 4),	
			new BlockRenderInfo(4, 2, 2, 2),	
			new BlockRenderInfo(4, 0, 2, 2),	
			new BlockRenderInfo(4, 2, 0, 2),	

			new BlockRenderInfo(4, 1, 1, 1),	
			new BlockRenderInfo(4, 0, 1, 1),	
			new BlockRenderInfo(4, 1, 0, 1),	

			new BlockRenderInfo(4, 1, 3, 1),	
			new BlockRenderInfo(4, 1, 2, 1),	
			new BlockRenderInfo(4, 0, 3, 1),	

			new BlockRenderInfo(4, 3, 1, 1),	
			new BlockRenderInfo(4, 3, 0, 1),	
			new BlockRenderInfo(4, 2, 1, 1),	

			new BlockRenderInfo(4, 3, 3, 1),	
			new BlockRenderInfo(4, 2, 3, 1),	
			new BlockRenderInfo(4, 3, 2, 1),	
	};
	
	private class BackgroundRenderer extends Thread {
		private boolean backgroundRunning;
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
		}

		public void run() {
			backgroundRunning = true;

			while (backgroundRunning) {
				DrawRequest currentDrawRequest = getNextDrawRequest();
				if (currentDrawRequest != null) {
					int block = 0;
					while (block < blockRenderInfos.length) {
						BlockRenderInfo blockRenderInfo = blockRenderInfos[block];
						calculateMandelbrot(currentDrawRequest, blockRenderInfo.blockSize, blockRenderInfo.pixelOffsetX, blockRenderInfo.pixelOffsetY, blockRenderInfo.pixelSize, MAX_ITERATION);
						Platform.runLater(() -> {
							drawMandelbrot();
						});
	
						DrawRequest anotherDrawRequest = getNextDrawRequest();
						if (anotherDrawRequest == null) {
							block++;
						} else {
							currentDrawRequest = anotherDrawRequest;
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
	
	private static final double KEY_TRANSLATE_FACTOR = 0.1;
	private static final double KEY_ZOOM_FACTOR = 1.2;

	private DoubleProperty xCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty yCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty zoomProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty radiusProperty = new SimpleDoubleProperty(2.0);
	private IntegerProperty paletteSeedProperty = new SimpleIntegerProperty(14);
	private IntegerProperty paletteStepProperty = new SimpleIntegerProperty(20);
	private IntegerProperty iterationsProperty = new SimpleIntegerProperty(01);
	
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
		GridPane gridPane = new GridPane();
        gridPane.setHgap(4);
        gridPane.setVgap(4);
		
		int rowIndex = 0;
		
		gridPane.add(new Label("X:"), 0, rowIndex);
		TextField xCenterTextField = new TextField();
		gridPane.add(xCenterTextField, 1, rowIndex);
		Bindings.bindBidirectional(xCenterTextField.textProperty(), xCenterProperty, DOUBLE_STRING_CONVERTER);
		rowIndex++;
		
		gridPane.add(new Label("Y:"), 0, rowIndex);
		TextField yCenterTextField = new TextField();
		gridPane.add(yCenterTextField, 1, rowIndex);
		Bindings.bindBidirectional(yCenterTextField.textProperty(), yCenterProperty, DOUBLE_STRING_CONVERTER);
		rowIndex++;
		
		gridPane.add(new Label("Zoom:"), 0, rowIndex);
//		Slider zoomSlider = new Slider(0.0, 10.0, 0.0);
//        zoomSlider.setShowTickMarks(true);
//        zoomSlider.setShowTickLabels(true);
//        zoomSlider.setMajorTickUnit(1.0f);
//        gridPane.add(zoomSlider, 1, rowIndex);
//        Bindings.bindBidirectional(zoomProperty, zoomSlider.valueProperty());
        TextField zoomTextField = new TextField();
		gridPane.add(zoomTextField, 1, rowIndex);
		Bindings.bindBidirectional(zoomTextField.textProperty(), zoomProperty, DOUBLE_STRING_CONVERTER);
		rowIndex++;

		gridPane.add(new Label("Radius:"), 0, rowIndex);
		TextField radiusTextField = new TextField();
		gridPane.add(radiusTextField, 1, rowIndex);
		Bindings.bindBidirectional(radiusTextField.textProperty(), radiusProperty, DOUBLE_STRING_CONVERTER);
		rowIndex++;

		gridPane.add(new Label("Iterations:"), 0, rowIndex);
		TextField iterationsTextField = new TextField();
		gridPane.add(iterationsTextField, 1, rowIndex);
		Bindings.bindBidirectional(iterationsTextField.textProperty(), iterationsProperty, INTEGER_FORMAT);
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
		
		return gridPane;
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
			zoomMandelbrot(canvas, 1.0 / event.getZoomFactor());
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
				zoomMandelbrot(canvas, 1.0/KEY_ZOOM_FACTOR);
				break;
			case DOWN:
				zoomMandelbrot(canvas, KEY_ZOOM_FACTOR);
				break;
			case W:
				translateMandelbrot(canvas, 0.0, -canvas.getHeight() * KEY_TRANSLATE_FACTOR);
				break;
			case A:
				translateMandelbrot(canvas, -canvas.getWidth() * KEY_TRANSLATE_FACTOR, 0.0);
				break;
			case S:
				translateMandelbrot(canvas, 0.0, canvas.getHeight() * KEY_TRANSLATE_FACTOR);
				break;
			case D:
				translateMandelbrot(canvas, canvas.getWidth() * KEY_TRANSLATE_FACTOR, 0.0);
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
		
		crosshairProperty.addListener((observable, oldValue, newValue) -> {
			drawMandelbrot();
		});
	}
	
	private void updatePalette(Canvas canvas) {
		palette = new CachingPalette(new RandomPalette(paletteSeedProperty.get(), paletteStepProperty.get()));
		calculateAndDrawMandelbrot(canvas);
	}

	private void translateMandelbrot(Canvas canvas, double deltaPixelX, double deltaPixelY) {
		double pixelWidth = canvas.getWidth();
		double pixelHeight = canvas.getHeight();

		double deltaX = deltaPixelX / pixelWidth * radiusProperty.get();
		double deltaY = deltaPixelY / pixelHeight * radiusProperty.get();
		
		xCenterProperty.set(xCenterProperty.get() + deltaX);
		yCenterProperty.set(yCenterProperty.get() + deltaY);
		
		calculateAndDrawMandelbrot(canvas);
	}

	private void zoomScrollMandelbrot(Canvas canvas, double deltaPixelY) {
		double pixelHeight = canvas.getHeight();
	
		double deltaZ = deltaPixelY / pixelHeight * 2.0;
		
		radiusProperty.set(radiusProperty.get() * (1.0 + deltaZ));

		calculateAndDrawMandelbrot(canvas);
	}

	private void zoomMandelbrot(Canvas canvas, double zoomFactor) {
		radiusProperty.set(radiusProperty.get() * zoomFactor);

		calculateAndDrawMandelbrot(canvas);
	}

	private void calculateAndDrawMandelbrot(Canvas canvas) {
		backgroundRenderer.triggerDraw(new DrawRequest(xCenterProperty.get(), yCenterProperty.get(), radiusProperty.get()));
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

	private void calculateMandelbrot(DrawRequest drawRequest, int blockSize, int blockPixelOffsetX, int blockPixelOffsetY, int pixelSize, int maxIteration) {
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double pixelWidth = image.getWidth();
		double pixelHeight = image.getHeight();

//		int centerPixelX = (int)pixelWidth / 2;
//		int centerPixelY = (int)pixelHeight / 2;
		double xRadius = drawRequest.radius;
		double yRadius = drawRequest.radius;
		double xCenter = drawRequest.x;
		double yCenter = drawRequest.y;
		
		double pixelStepX = xRadius*2 / pixelWidth;
		double pixelStepY = yRadius*2 / pixelHeight;
		double blockStepX = pixelStepX * blockSize;
		double blockStepY = pixelStepY * blockSize;
		double x0 = pixelStepX * blockPixelOffsetX - xCenter - xRadius;
		
		for (int pixelX = blockPixelOffsetX; pixelX < pixelWidth; pixelX+=blockSize) {
			x0 += blockStepX;
			double y0 = pixelStepY * blockPixelOffsetY - yCenter - yRadius; 
			for (int pixelY = blockPixelOffsetY; pixelY < pixelHeight; pixelY+=blockSize) {
				y0 += blockStepY;
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

//				if (pixelX == centerPixelX && pixelY == centerPixelY) {
//					iterationsProperty.set(iteration);
//				}
				
				Color color = iteration == maxIteration ? Color.BLACK : palette.getColor(iteration);
				for (int pixelOffsetX = 0; pixelOffsetX < pixelSize; pixelOffsetX++) {
					for (int pixelOffsetY = 0; pixelOffsetY < pixelSize; pixelOffsetY++) {
						pixelWriter.setColor(pixelX + pixelOffsetX, pixelY + pixelOffsetY, color);
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

}
