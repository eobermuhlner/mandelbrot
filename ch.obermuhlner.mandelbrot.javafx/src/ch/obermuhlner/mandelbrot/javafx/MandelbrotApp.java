package ch.obermuhlner.mandelbrot.javafx;

import java.util.stream.Stream;

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

	private static final int MAX_ITERATION = 100;
	
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
		
		double height = 800;
		double width = 800;
		Canvas canvas = new Canvas(width, height);
		drawMandelbrot(canvas, 1);
		
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
			
			translateMandelbrot(canvas, deltaX, deltaY, 2);
		});
		canvas.setOnMouseReleased(event -> {
			double deltaX = event.getX() - lastMouseDragX;
			double deltaY = event.getY() - lastMouseDragY;
			lastMouseDragX = event.getX();
			lastMouseDragY = event.getY();

			translateMandelbrot(canvas, deltaX, deltaY, 1);
		});
		
		canvas.setOnScroll(event -> {
			double deltaY = event.getDeltaY();
			
			zoomMandelbrot(canvas, deltaY, 1);
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

	private void zoomMandelbrot(Canvas canvas, double deltaPixelY, int pixelSize) {
		double pixelHeight = canvas.getHeight();
	
		double deltaZ = deltaPixelY / pixelHeight * 2.0;
		
		radiusProperty.set(radiusProperty.get() * (1.0 + deltaZ));

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
