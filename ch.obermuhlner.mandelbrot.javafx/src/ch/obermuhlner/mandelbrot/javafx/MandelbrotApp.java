package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import ch.obermuhlner.mandelbrot.javafx.palette.CachingPalette;
import ch.obermuhlner.mandelbrot.javafx.palette.CyclingPalette;
import ch.obermuhlner.mandelbrot.javafx.palette.HuePalette;
import ch.obermuhlner.mandelbrot.javafx.palette.InterpolatingPalette;
import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import ch.obermuhlner.mandelbrot.javafx.palette.RandomPalette;
import ch.obermuhlner.mandelbrot.poi.PointOfInterest;
import ch.obermuhlner.mandelbrot.poi.StandardPointsOfInterest;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
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
	private static final double SCROLL_ZOOM_STEP = 0.5;

	private static final int IMAGE_SIZE = 512+256;
	
	private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("##0");
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("##0.000");
	private static final DecimalFormat DOUBLE_8DIGITS_FORMAT = new DecimalFormat("##0.00000000");
	
	private static final Color WHITE_90 = new Color(1.0, 1.0, 1.0, 0.9);
	private static final Color WHITE_20 = new Color(1.0, 1.0, 1.0, 0.2);

	private enum PaletteType {
		RandomColor,
		RandomGray,
		RandomPastell,
		Fire,
		Water,
		Air,
		Earth,
		Forest,
		StarryNight,
		Drawing,
		Rainbow,
	}
	
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
	
	private ObjectProperty<BigDecimal> xCenterProperty = new SimpleObjectProperty<BigDecimal>(BigDecimal.ZERO);
	private ObjectProperty<BigDecimal> yCenterProperty = new SimpleObjectProperty<BigDecimal>(BigDecimal.ZERO);
	private DoubleProperty zoomProperty = new SimpleDoubleProperty(0.0);
	
	private ObjectProperty<PaletteType> paletteTypeProperty = new SimpleObjectProperty<PaletteType>(PaletteType.RandomColor);
	private IntegerProperty paletteSeedProperty = new SimpleIntegerProperty(14);
	private IntegerProperty paletteStepProperty = new SimpleIntegerProperty(20);
	
	private BooleanProperty crosshairProperty = new SimpleBooleanProperty(true); 
	private BooleanProperty gridProperty = new SimpleBooleanProperty(false); 

	private Palette palette;

	private Canvas mandelbrotCanvas;
	private WritableImage image = new WritableImage(IMAGE_SIZE, IMAGE_SIZE);
	
	private BackgroundProgressiveRenderer backgroundProgressiveRenderer;
	private BackgroundSnapshotRenderer backgroundSnapshotRenderer;

	@Override
	public void start(Stage primaryStage) throws Exception {
		backgroundProgressiveRenderer = new BackgroundProgressiveRenderer(this);
		backgroundProgressiveRenderer.start();

		backgroundSnapshotRenderer = new BackgroundSnapshotRenderer();
		backgroundSnapshotRenderer.start();
		
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
		
		Node status = createStatus();
		borderPane.setBottom(status);
		
		primaryStage.setScene(scene);
		primaryStage.show();
		
		mandelbrotCanvas.requestFocus();
	}

	@Override
	public void stop() throws Exception {
		backgroundProgressiveRenderer.stopRunning();
		backgroundSnapshotRenderer.stopRunning();
		
		super.stop();
	}
	
	private Canvas createMandelbrotCanvas() {
		double height = IMAGE_SIZE;
		double width = IMAGE_SIZE;
		
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
		
		ToggleButton gridToggleButton = new ToggleButton("Grid");
		box.getChildren().add(gridToggleButton);
		Bindings.bindBidirectional(gridToggleButton.selectedProperty(), gridProperty);
		
		ComboBox<PointOfInterest> pointsOfInterestComboBox = new ComboBox<>(FXCollections.observableList(Arrays.asList(StandardPointsOfInterest.POINTS_OF_INTEREST)));
		box.getChildren().add(pointsOfInterestComboBox);
		pointsOfInterestComboBox.setValue(StandardPointsOfInterest.POINTS_OF_INTEREST[0]);
		pointsOfInterestComboBox.setOnAction(event -> {
			PointOfInterest pointOfInterest = pointsOfInterestComboBox.getValue();
			xCenterProperty.set(pointOfInterest.x);
			yCenterProperty.set(pointOfInterest.y);
			zoomProperty.set(pointOfInterest.zoom);
			paletteSeedProperty.set(pointOfInterest.paletteSeed);
			paletteStepProperty.set(pointOfInterest.paletteStep);
		});
		
		Button snapshotButton = new Button("Snapshot");
		box.getChildren().add(snapshotButton);
		snapshotButton.setOnAction(event -> {
			DrawRequest drawRequest = new DrawRequest(xCenterProperty.get(), yCenterProperty.get(), zoomProperty.get());
			String filename = "mandelbrot" + LocalDateTime.now().toString().replace(':', '_') + ".png";
			backgroundSnapshotRenderer.addSnapshotRequest(new SnapshotRequest(drawRequest, palette, new File(filename)));
		});
		
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

		gridPane.add(new Label("Color Palette:"), 0, rowIndex);
		ComboBox<PaletteType> paletteTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(PaletteType.values()));
		gridPane.add(paletteTypeComboBox, 1, rowIndex);
		Bindings.bindBidirectional(paletteTypeComboBox.valueProperty(), paletteTypeProperty);
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

	private Node createStatus() {
		TableView<SnapshotRequest> snapshotTableView = new TableView<>(backgroundSnapshotRenderer.getSnapshotRequests());
		snapshotTableView.setPrefHeight(100);
		addTableColumn(snapshotTableView, "File", 250, snapshotRequest -> {
			return new ReadOnlyStringWrapper(snapshotRequest.file.getName());
		});
		addTableColumn(snapshotTableView, "Status", 100, snapshotRequest -> {
			return snapshotRequest.snapshotStatusProperty();
		});
		addProgressBarTableColumn(snapshotTableView, "Progress", 100, snapshotRequest -> {
			return snapshotRequest.progressProperty().asObject();
		});
		addTableColumn(snapshotTableView, "%", 60, snapshotRequest -> {
			return snapshotRequest.progressProperty().multiply(100);
		});
		addTableColumn(snapshotTableView, "Calculation Time", 100, snapshotRequest -> {
			return snapshotRequest.calculationMillisProperty();
		});
		addTableColumn(snapshotTableView, "X", 100, snapshotRequest -> {
			return new ReadOnlyStringWrapper(DOUBLE_8DIGITS_FORMAT.format(snapshotRequest.drawRequest.x));
		});
		addTableColumn(snapshotTableView, "Y", 100, snapshotRequest -> {
			return new ReadOnlyStringWrapper(DOUBLE_8DIGITS_FORMAT.format(snapshotRequest.drawRequest.y));
		});
		addTableColumn(snapshotTableView, "Zoom", 60, snapshotRequest -> {
			return new ReadOnlyStringWrapper(DOUBLE_FORMAT.format(snapshotRequest.drawRequest.zoom));
		});
		addTableColumn(snapshotTableView, "Width", 60, snapshotRequest -> {
			return new ReadOnlyStringWrapper(INTEGER_FORMAT.format(snapshotRequest.width));
		});
		addTableColumn(snapshotTableView, "Height", 60, snapshotRequest -> {
			return new ReadOnlyStringWrapper(INTEGER_FORMAT.format(snapshotRequest.height));
		});

		return snapshotTableView;
	}

	private <E, V> TableColumn<E, V> addTableColumn(TableView<E> tableView, String header, double prefWidth, Function<E, ObservableValue<V>> valueFunction) {
		TableColumn<E, V> column = new TableColumn<>(header);
		column.setPrefWidth(prefWidth);
		column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<E,V>, ObservableValue<V>>() {
			@Override
			public ObservableValue<V> call(CellDataFeatures<E, V> cellData) {
				return valueFunction.apply(cellData.getValue());
			}
		});
		tableView.getColumns().add(column);
		return column;
	}
	
	private <E> TableColumn<E, Double> addProgressBarTableColumn(TableView<E> tableView, String header, double prefWidth, Function<E, ObservableValue<Double>> valueFunction) {
		TableColumn<E, Double> column = addTableColumn(tableView, header, prefWidth, valueFunction);
		column.setCellFactory(new Callback<TableColumn<E,Double>, TableCell<E,Double>>() {
			@Override
			public TableCell<E, Double> call(TableColumn<E, Double> param) {
				return new ProgressBarTableCell<E>();
			}
		});
		return column;
	}
	
	double lastMouseDragX;
	double lastMouseDragY;
	private void setupCanvasEventHandlers(Canvas canvas) {
		canvas.setOnMousePressed(event -> {
			canvas.requestFocus();
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

		paletteTypeProperty.addListener((observable, oldValue, newValue) -> {
			updatePalette(canvas);
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
		gridProperty.addListener((observable, oldValue, newValue) -> {
			drawMandelbrot();
		});
	}
	
	private void updatePalette(Canvas canvas) {
		int seed = paletteSeedProperty.get();
		int steps = paletteStepProperty.get();
		if (steps <= 0) {
			steps = 10;
		}
		
		switch (paletteTypeProperty.get()) {
		case RandomColor:
			palette = new CachingPalette(new InterpolatingPalette(new RandomPalette(seed), steps));
			break;
		case RandomGray:
			palette = new CachingPalette(new InterpolatingPalette(new RandomPalette(seed, 0f, 360f, 0.0f, 0.0f, 0.2f, 1.0f), steps));
			break;
		case RandomPastell:
			palette = new CachingPalette(new InterpolatingPalette(new RandomPalette(seed, 0f, 360f, 0.0f, 0.3f, 0.2f, 1.0f), steps));
			break;
		case Drawing:
			palette = new CyclingPalette(Color.WHITE, steps, Color.gray(0.8), Color.gray(0.6), Color.gray(0.4), Color.gray(0.2), Color.gray(0.0), Color.gray(0.2), Color.gray(0.4), Color.gray(0.6), Color.gray(0.8));
			break;
		case Fire:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.RED, Color.YELLOW, Color.DARKRED, Color.ORANGE, Color.gray(0.1)), steps));
			break;
		case Water:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.BLUE, Color.LIGHTBLUE, Color.DARKBLUE, Color.CYAN, Color.gray(0.1)), steps));
			break;
		case Air:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.LIGHTBLUE, Color.WHITE, Color.BLUE, Color.LIGHTBLUE.brighter(), Color.WHITE, Color.CYAN), steps));
			break;
		case Earth:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.SADDLEBROWN, Color.GREEN, Color.DARKGREEN, Color.BROWN.darker(), Color.SANDYBROWN), steps));
			break;
		case Forest:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.GREENYELLOW, Color.GREEN, Color.DARKGREEN, Color.LIGHTGREEN, Color.gray(0.1)), steps));
			break;
		case StarryNight:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.gray(0.1), Color.WHITE, Color.gray(0.1)), steps));
			break;
		case Rainbow:
			palette = new CachingPalette(new HuePalette(steps));
			break;
		}
		
		calculateAndDrawMandelbrot(canvas);
	}
	
	private void translateMandelbrot(Canvas canvas, double deltaPixelX, double deltaPixelY) {
		BigDecimal pixelWidth = BigDecimal.valueOf(canvas.getWidth());
		BigDecimal pixelHeight = BigDecimal.valueOf(canvas.getHeight());

		
		BigDecimal radius = MandelbrotMath.getRadius(zoomProperty.get());
		BigDecimal deltaX = BigDecimal.valueOf(deltaPixelX).divide(pixelWidth, MathContext.DECIMAL128).multiply(radius);
		BigDecimal deltaY = BigDecimal.valueOf(deltaPixelY).divide(pixelHeight, MathContext.DECIMAL128).multiply(radius);

		setCoordinates(xCenterProperty.get().add(deltaX), yCenterProperty.get().add(deltaY));
		
		calculateAndDrawMandelbrot(canvas);
	}
	
	public void setCoordinates(BigDecimal xCenter, BigDecimal yCenter) {
		xCenterProperty.set(xCenter);
		yCenterProperty.set(yCenter);
	}

	private void zoomScrollMandelbrot(Canvas canvas, double deltaPixelY) {
		double pixelHeight = canvas.getHeight();
	
		double deltaZ = deltaPixelY / pixelHeight * 2.0;

		zoomProperty.set(zoomProperty.get() + deltaZ * SCROLL_ZOOM_STEP); // FIXME

		calculateAndDrawMandelbrot(canvas);
	}

	private void zoomMandelbrot(Canvas canvas, double zoomStep) {
		zoomProperty.set(zoomProperty.get() + zoomStep);

		calculateAndDrawMandelbrot(canvas);
	}
	
	private void calculateAndDrawMandelbrot(Canvas canvas) {
		backgroundProgressiveRenderer.triggerDraw(new DrawRequest(xCenterProperty.get(), yCenterProperty.get(), zoomProperty.get()));
	}		
	
	void drawMandelbrot() {
		GraphicsContext gc = mandelbrotCanvas.getGraphicsContext2D();
		gc.drawImage(image, 0, 0);

		if (crosshairProperty.get()) {
			gc.setStroke(WHITE_90);
			gc.strokeLine(mandelbrotCanvas.getWidth() / 2, 0, mandelbrotCanvas.getWidth() / 2, mandelbrotCanvas.getHeight());
			gc.strokeLine(0, mandelbrotCanvas.getHeight() / 2, mandelbrotCanvas.getWidth(), mandelbrotCanvas.getHeight() / 2);
		}

		if (gridProperty.get()) {
			gc.setStroke(WHITE_20);
			int n = 20;
			for (int i = 0; i < n; i++) {
				gc.strokeLine(mandelbrotCanvas.getWidth() / n * i, 0, mandelbrotCanvas.getWidth() / n * i, mandelbrotCanvas.getHeight());
				gc.strokeLine(0, mandelbrotCanvas.getHeight() / n * i, mandelbrotCanvas.getWidth(), mandelbrotCanvas.getHeight() / n * i);
			}
		}
	}

	void calculateMandelbrot(DrawRequest drawRequest, int blockSize, int blockPixelOffsetX, int blockPixelOffsetY, int pixelSize) {
		if (drawRequest.isInsideDoublePrecision()) {
			calculateMandelbrotDouble(drawRequest, blockSize, blockPixelOffsetX, blockPixelOffsetY, pixelSize);
		} else {
			calculateMandelbrotBigDecimal(drawRequest, blockSize, blockPixelOffsetX, blockPixelOffsetY, pixelSize, true);
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
				int iterations = MandelbrotMath.calculateMandelbrotIterations(x0, y0, maxIteration);

				Color color = iterations == maxIteration ? Color.BLACK : palette.getColor(iterations);
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

	private void calculateMandelbrotBigDecimal(DrawRequest drawRequest, int blockSize, int blockPixelOffsetX, int blockPixelOffsetY, int pixelSize, boolean parallel) {
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double pixelWidth = image.getWidth();
		double pixelHeight = image.getHeight();
		
		MathContext mc = new MathContext(drawRequest.getPrecision());

		BigDecimal xRadius = drawRequest.getRadius();
		BigDecimal yRadius = xRadius;
		BigDecimal xCenter = drawRequest.x;
		BigDecimal yCenter = drawRequest.y;
		int maxIteration = drawRequest.getMaxIteration();
		
		BigDecimal two = new BigDecimal(2);
		BigDecimal pixelStepX = xRadius.multiply(two, mc).divide(BigDecimal.valueOf(pixelWidth), mc);
		BigDecimal pixelStepY = yRadius.multiply(two, mc).divide(BigDecimal.valueOf(pixelHeight), mc);
		BigDecimal blockStepX = pixelStepX.multiply(new BigDecimal(blockSize), mc);
		BigDecimal blockStepY = pixelStepY.multiply(new BigDecimal(blockSize), mc);
		BigDecimal x0Start = pixelStepX.multiply(new BigDecimal(blockPixelOffsetX), mc).subtract(xCenter, mc).subtract(xRadius, mc);

		IntStream range = IntStream.range(0, (int)(pixelWidth / blockSize));
		if (parallel) {
			range = range.parallel();
		}
		range.forEach(indexPixelX -> {
			int pixelX = blockPixelOffsetX + indexPixelX * blockSize;
			BigDecimal x0 = x0Start.add(blockStepX.multiply(new BigDecimal(indexPixelX), mc), mc);
			BigDecimal y0 = pixelStepY.multiply(new BigDecimal(blockPixelOffsetY), mc).subtract(yCenter, mc).subtract(yRadius, mc);
			for (int pixelY = blockPixelOffsetY; pixelY < pixelHeight; pixelY+=blockSize) {
				int iterations = MandelbrotMath.calculateMandelbrotIterations(x0, y0, maxIteration, mc);
				
				Color color = iterations == maxIteration ? Color.BLACK : palette.getColor(iterations);
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
		});
	}

	public static void main(String[] args) {
		launch(args);
	}

}
