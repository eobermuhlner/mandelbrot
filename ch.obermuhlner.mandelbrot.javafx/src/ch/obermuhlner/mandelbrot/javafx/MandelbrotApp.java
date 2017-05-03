package ch.obermuhlner.mandelbrot.javafx;

import java.text.DecimalFormat;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MandelbrotApp extends Application {

	private static final int MAX_ITERATION = 100;
	
	private static final Color[] PALETTE = new Color[MAX_ITERATION+1];
	
	static {
		for (int i = 0; i < PALETTE.length; i++) {
			double factor = 1.0 / MAX_ITERATION * i;
			double correctedFactor = Math.sqrt(factor);
			PALETTE[i] = Color.hsb(correctedFactor * 360, 1.0, 1.0 - correctedFactor);
		}
	}

	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("##0.00000000");

	private DoubleProperty xCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty yCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty radiusProperty = new SimpleDoubleProperty(2.0);

	@Override
	public void start(Stage primaryStage) throws Exception {
		Group root = new Group();
		Scene scene = new Scene(root);
		
		BorderPane borderPane = new BorderPane();
		root.getChildren().add(borderPane);
		
		Node toolbar = createToolbar();
		borderPane.setTop(toolbar);

		Canvas canvas = createMandelbrotCanvas();
		borderPane.setCenter(canvas);
		
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private Canvas createMandelbrotCanvas() {
		double height = 800;
		double width = 800;
		
		Canvas canvas = new Canvas(width, height);
		drawMandelbrot(canvas, 1);

		setupCanvasEventHandlers(canvas);

		return canvas;
	}

	private Node createToolbar() {
		HBox toolbar = new HBox();
		
		TextField xCenterTextField = new TextField();
		toolbar.getChildren().add(xCenterTextField);
		Bindings.bindBidirectional(xCenterTextField.textProperty(), xCenterProperty, DOUBLE_FORMAT);
		
		TextField yCenterTextField = new TextField();
		toolbar.getChildren().add(yCenterTextField);
		Bindings.bindBidirectional(yCenterTextField.textProperty(), yCenterProperty, DOUBLE_FORMAT);
		
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
			
			translateMandelbrot(canvas, deltaX, deltaY, 2);
		});
		canvas.setOnMouseReleased(event -> {
			double deltaX = event.getX() - lastMouseDragX;
			double deltaY = event.getY() - lastMouseDragY;
			lastMouseDragX = event.getX();
			lastMouseDragY = event.getY();

			translateMandelbrot(canvas, deltaX, deltaY, 1);
		});
		
		canvas.setOnZoom(event -> {
			zoomMandelbrot(canvas, 1.0 / event.getZoomFactor(), 2);
		});
		canvas.setOnZoomFinished(event -> {
			drawMandelbrot(canvas, 1);
		});
		
		canvas.setOnScroll(event -> {
			if (!event.isDirect()) {
				double deltaY = event.getDeltaY();
				
				zoomScrollMandelbrot(canvas, deltaY, 1);
			}
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
		GraphicsContext gc = canvas.getGraphicsContext2D();
		
		double pixelWidth = canvas.getWidth();
		double pixelHeight = canvas.getHeight();
		
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
				while (xx + yy < 2*2 && iteration < MAX_ITERATION) {
					y = 2*x*y + y0;
					x = xx - yy + x0;
					iteration++;
					
					xx = x*x;
					yy = y*y;
				}

				gc.setFill(PALETTE[iteration]);
				gc.fillRect(pixelX, pixelY, pixelSize, pixelSize);
			}
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

}
