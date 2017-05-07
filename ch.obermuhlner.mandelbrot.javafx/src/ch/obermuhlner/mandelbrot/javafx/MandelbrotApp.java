package ch.obermuhlner.mandelbrot.javafx;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


/*
 * Nice points:
 * 0.56255802552 0.65043192728
 * -0.04729622199 0.66103581600
 */
public class MandelbrotApp extends Application {

	private static final int MAX_ITERATION = 1000;

	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("##0.00000000000");

	private static final double KEY_TRANSLATE_FACTOR = 0.1;
	private static final double KEY_ZOOM_FACTOR = 1.2;

	private static final int GOOD_QUALITY = 1;
	private static final int MEDIUM_QUALITY = 4;
	
	private DoubleProperty xCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty yCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty radiusProperty = new SimpleDoubleProperty(2.0);

	private Palette palette = new CachingPalette(new RandomPalette(3, 20));
	
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

		drawMandelbrot(canvas, GOOD_QUALITY);

		return canvas;
	}

	private Node createToolbar() {
		HBox toolbar = new HBox(2);
		toolbar.setAlignment(Pos.CENTER_LEFT);
		
		toolbar.getChildren().add(new Label("X:"));
		TextField xCenterTextField = new TextField();
		toolbar.getChildren().add(xCenterTextField);
		Bindings.bindBidirectional(xCenterTextField.textProperty(), xCenterProperty, DOUBLE_FORMAT);
		
		toolbar.getChildren().add(new Label("Y:"));
		TextField yCenterTextField = new TextField();
		toolbar.getChildren().add(yCenterTextField);
		Bindings.bindBidirectional(yCenterTextField.textProperty(), yCenterProperty, DOUBLE_FORMAT);
		
		toolbar.getChildren().add(new Label("Radius:"));
		TextField radiusTextField = new TextField();
		toolbar.getChildren().add(radiusTextField);
		Bindings.bindBidirectional(radiusTextField.textProperty(), radiusProperty, DOUBLE_FORMAT);
		
		return toolbar;
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
			drawMandelbrot(canvas, GOOD_QUALITY);
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
	}

	private void translateMandelbrot(Canvas canvas, double deltaPixelX, double deltaPixelY, int pixelSize) {
		double pixelWidth = canvas.getWidth();
		double pixelHeight = canvas.getHeight();

		double deltaX = deltaPixelX / pixelWidth * radiusProperty.get();
		double deltaY = deltaPixelY / pixelHeight * radiusProperty.get();
		
		xCenterProperty.set(xCenterProperty.get() + deltaX);
		yCenterProperty.set(yCenterProperty.get() + deltaY);
		
		drawMandelbrot(canvas, pixelSize);
	}

	private void zoomScrollMandelbrot(Canvas canvas, double deltaPixelY, int pixelSize) {
		double pixelHeight = canvas.getHeight();
	
		double deltaZ = deltaPixelY / pixelHeight * 2.0;
		
		radiusProperty.set(radiusProperty.get() * (1.0 + deltaZ));

		drawMandelbrot(canvas, pixelSize);
	}

	private void zoomMandelbrot(Canvas canvas, double zoomFactor, int pixelSize) {
		radiusProperty.set(radiusProperty.get() * zoomFactor);

		drawMandelbrot(canvas, pixelSize);
	}

	private void drawMandelbrot(Canvas canvas, int pixelSize) {
		drawMandelbrot(pixelSize, MAX_ITERATION);
		
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.drawImage(image, 0, 0);
	}

	private void drawMandelbrot(int pixelSize, int maxIteration) {
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double pixelWidth = image.getWidth();
		double pixelHeight = image.getHeight();
		
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
