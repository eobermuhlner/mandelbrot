package ch.obermuhlner.mandelbrot.javafx;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MandelbrotApp extends Application {

	private static final int MAX_ITERATION = 1000;
	
	private static final Color[] PALETTE = new Color[MAX_ITERATION+1];
	
	static {
		for (int i = 0; i < PALETTE.length; i++) {
			double factor = 1.0 / MAX_ITERATION * i;
			double correctedFactor = Math.sqrt(factor);
			PALETTE[i] = Color.hsb(correctedFactor * 360, 1.0, 1.0 - correctedFactor);
		}
	}
	
	private DoubleProperty xCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty yCenterProperty = new SimpleDoubleProperty(0.0);
	private DoubleProperty radiusProperty = new SimpleDoubleProperty(2.0);

	@Override
	public void start(Stage primaryStage) throws Exception {
		Group root = new Group();
		Scene scene = new Scene(root);
		
		double height = 400;
		double width = 400;
		Canvas canvas = new Canvas(width, height);
		drawMandelbrot(canvas);
		
		root.getChildren().add(canvas);
		
		primaryStage.setScene(scene);
		primaryStage.show();

		setupCanvasEventHandlers(canvas);
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
		
		canvas.setOnScroll(event -> {
			double deltaY = event.getDeltaY();
			
			zoomMandelbrot(canvas, deltaY);
		});
	}

	private void translateMandelbrot(Canvas canvas, double deltaPixelX, double deltaPixelY) {
		double pixelWidth = canvas.getWidth();
		double pixelHeight = canvas.getHeight();

		double deltaX = deltaPixelX / pixelWidth * radiusProperty.get();
		double deltaY = deltaPixelY / pixelHeight * radiusProperty.get();
		
		xCenterProperty.set(xCenterProperty.get() + deltaX);
		yCenterProperty.set(yCenterProperty.get() + deltaY);
		
		drawMandelbrot(canvas);
	}

	private void zoomMandelbrot(Canvas canvas, double deltaPixelY) {
		double pixelHeight = canvas.getHeight();
	
		double deltaZ = deltaPixelY / pixelHeight;
		
		radiusProperty.set(radiusProperty.get() * (1.0 + deltaZ));

		drawMandelbrot(canvas);
	}


	private void drawMandelbrot(Canvas canvas) {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		
		double pixelWidth = canvas.getWidth();
		double pixelHeight = canvas.getHeight();
		
		double xRadius = radiusProperty.get();
		double yRadius = radiusProperty.get();
		double xCenter = xCenterProperty.get();
		double yCenter = yCenterProperty.get();
		
		for (int pixelX = 0; pixelX < pixelWidth; pixelX++) {
			for (int pixelY = 0; pixelY < pixelHeight; pixelY++) {
				double x0 = pixelX * xRadius*2 / pixelWidth - xCenter - xRadius;
				double y0 = pixelY * yRadius*2 / pixelHeight - yCenter - yRadius;
				double x = 0;
				double y = 0;
				int iteration = 0;
				while (x*x + y*y < 2*2 && iteration < MAX_ITERATION) {
					double tmp = x*x - y*y + x0;
					y = 2*x*y + y0;
					x = tmp;
					iteration++;
				}

				gc.setFill(PALETTE[iteration]);
				gc.fillRect(pixelX, pixelY, 1, 1);
			}
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

}
