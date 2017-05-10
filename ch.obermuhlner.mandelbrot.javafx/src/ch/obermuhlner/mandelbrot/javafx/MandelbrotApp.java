package ch.obermuhlner.mandelbrot.javafx;

import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
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

	private static final double KEY_TRANSLATE_FACTOR = 0.1;
	private static final double KEY_ZOOM_FACTOR = 1.2;

	private static final int GOOD_QUALITY = 1;
	private static final int MEDIUM_QUALITY = 4;
	
	private DoubleProperty xCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty yCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty zoomProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty radiusProperty = new SimpleDoubleProperty(2.0);
	private IntegerProperty paletteSeedProperty = new SimpleIntegerProperty(14);
	private IntegerProperty paletteStepProperty = new SimpleIntegerProperty(20);
	private IntegerProperty iterationsProperty = new SimpleIntegerProperty(01);
	
	private BooleanProperty crosshairProperty = new SimpleBooleanProperty(true); 

	private Palette palette;
	
	private WritableImage image = new WritableImage(800, 800);
	private volatile DrawRequest drawRequest;
	
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		Group root = new Group();
		Scene scene = new Scene(root);
		
		BorderPane borderPane = new BorderPane();
		root.getChildren().add(borderPane);
		
		Node toolbar = createToolbar();
		borderPane.setTop(toolbar);

		Node editor = createEditor();
		borderPane.setRight(editor);

		Canvas mandelbrotCanvas = createMandelbrotCanvas();
		borderPane.setCenter(mandelbrotCanvas);
		
		primaryStage.setScene(scene);
		primaryStage.show();
		
		mandelbrotCanvas.requestFocus();
	}

	@Override
	public void stop() throws Exception {
		executor.shutdown();
		
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
			
			translateMandelbrot(canvas, deltaX, deltaY, MEDIUM_QUALITY);
		});
		canvas.setOnMouseReleased(event -> {
			double deltaX = event.getX() - lastMouseDragX;
			double deltaY = event.getY() - lastMouseDragY;
			lastMouseDragX = event.getX();
			lastMouseDragY = event.getY();

			translateMandelbrot(canvas, deltaX, deltaY, GOOD_QUALITY);
		});
	
		canvas.setOnZoom(event -> {
			zoomMandelbrot(canvas, 1.0 / event.getZoomFactor(), MEDIUM_QUALITY);
		});
		canvas.setOnZoomFinished(event -> {
			calculateAndDrawMandelbrot(canvas, GOOD_QUALITY);
		});
		
		canvas.setOnScroll(event -> {
			if (!event.isDirect()) {
				double deltaY = event.getDeltaY();
				
				zoomScrollMandelbrot(canvas, deltaY, GOOD_QUALITY);
			}
		});
		
		canvas.setOnKeyPressed(event -> {
			switch (event.getCode()) {
			case UP:
				zoomMandelbrot(canvas, 1.0/KEY_ZOOM_FACTOR, GOOD_QUALITY);
				break;
			case DOWN:
				zoomMandelbrot(canvas, KEY_ZOOM_FACTOR, GOOD_QUALITY);
				break;
			case W:
				translateMandelbrot(canvas, 0.0, -canvas.getHeight() * KEY_TRANSLATE_FACTOR, 1);
				break;
			case A:
				translateMandelbrot(canvas, -canvas.getWidth() * KEY_TRANSLATE_FACTOR, 0.0, 1);
				break;
			case S:
				translateMandelbrot(canvas, 0.0, canvas.getHeight() * KEY_TRANSLATE_FACTOR, 1);
				break;
			case D:
				translateMandelbrot(canvas, canvas.getWidth() * KEY_TRANSLATE_FACTOR, 0.0, 1);
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
			drawMandelbrot(canvas);
		});
	}
	
	private void updatePalette(Canvas canvas) {
		palette = new CachingPalette(new RandomPalette(paletteSeedProperty.get(), paletteStepProperty.get()));
		calculateAndDrawMandelbrot(canvas, GOOD_QUALITY);
	}

	private void translateMandelbrot(Canvas canvas, double deltaPixelX, double deltaPixelY, int pixelSize) {
		double pixelWidth = canvas.getWidth();
		double pixelHeight = canvas.getHeight();

		double deltaX = deltaPixelX / pixelWidth * radiusProperty.get();
		double deltaY = deltaPixelY / pixelHeight * radiusProperty.get();
		
		xCenterProperty.set(xCenterProperty.get() + deltaX);
		yCenterProperty.set(yCenterProperty.get() + deltaY);
		
		calculateAndDrawMandelbrot(canvas, pixelSize);
	}

	private void zoomScrollMandelbrot(Canvas canvas, double deltaPixelY, int pixelSize) {
		double pixelHeight = canvas.getHeight();
	
		double deltaZ = deltaPixelY / pixelHeight * 2.0;
		
		radiusProperty.set(radiusProperty.get() * (1.0 + deltaZ));

		calculateAndDrawMandelbrot(canvas, pixelSize);
	}

	private void zoomMandelbrot(Canvas canvas, double zoomFactor, int pixelSize) {
		radiusProperty.set(radiusProperty.get() * zoomFactor);

		calculateAndDrawMandelbrot(canvas, pixelSize);
	}

	private void calculateAndDrawMandelbrot(Canvas canvas, int pixelSize) {
		calculateMandelbrot(pixelSize, MAX_ITERATION);
		
		drawMandelbrot(canvas);
	}		

	private void drawMandelbrot(Canvas canvas) {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.drawImage(image, 0, 0);

		if (crosshairProperty.get()) {
			gc.setStroke(Color.WHITE);
			gc.strokeLine(canvas.getWidth() / 2, 0, canvas.getWidth() / 2, canvas.getHeight());
			gc.strokeLine(0, canvas.getHeight() / 2, canvas.getWidth(), canvas.getHeight() / 2);
		}
	}

	private void calculateMandelbrot(int pixelSize, int maxIteration) {
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double pixelWidth = image.getWidth();
		double pixelHeight = image.getHeight();

		int centerPixelX = (int)pixelWidth / 2;
		int centerPixelY = (int)pixelHeight / 2;
		double xRadius = radiusProperty.get();
		double yRadius = radiusProperty.get();
		double xCenter = xCenterProperty.get();
		double yCenter = yCenterProperty.get();
		
		double stepX = xRadius*2 / pixelWidth * pixelSize;
		double stepY = yRadius*2 / pixelHeight * pixelSize;
		double x0 = 0 - xCenter - xRadius;
		
		for (int pixelX = 0; pixelX < pixelWidth; pixelX+=pixelSize) {
			x0 += stepX;
			double y0 = 0 - yCenter - yRadius; 
			for (int pixelY = 0; pixelY < pixelHeight; pixelY+=pixelSize) {
				y0 += stepY;
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

				if (pixelX == centerPixelX && pixelY == centerPixelY) {
					iterationsProperty.set(iteration);
				}
				
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
