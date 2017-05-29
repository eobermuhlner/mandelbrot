package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.IntStream;

import ch.obermuhlner.mandelbrot.javafx.palette.CachingPalette;
import ch.obermuhlner.mandelbrot.javafx.palette.CyclingPalette;
import ch.obermuhlner.mandelbrot.javafx.palette.HuePalette;
import ch.obermuhlner.mandelbrot.javafx.palette.InterpolatingPalette;
import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import ch.obermuhlner.mandelbrot.javafx.palette.RandomPalette;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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

	private static final int IMAGE_SIZE = 512+256;
	
	private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("##0");
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("##0.000");
	
	private static final Color WHITE_90 = new Color(1.0, 1.0, 1.0, 0.9);
	private static final Color WHITE_20 = new Color(1.0, 1.0, 1.0, 0.2);

	private enum PaletteType {
		RandomColor,
		RandomGray,
		RandomPastell,
		IsoLine,
		Fire,
		Ice,
		Forest,
		StarryNight,
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

	private static PointOfInterest[] POINTS_OF_INTEREST = {
			new PointOfInterest(
					"Curved Swords",
					new BigDecimal("0.017919288259557892593847458731170858208746794667084869140010189701941671977885175491969822815680324822695703303994612753773916901726488514999932436890"),
					new BigDecimal("1.011760975319870618534630909564629408391755036843653640355850940201872641222527031793665593388080854231137835177185531026500538033949104973217479327210"),
					10.9, // until 101
					1,
					10),
			new PointOfInterest(
					"Snail Shell",
					new BigDecimal("1.74972192974233857132851218320479685207010404588970504566538777023661204494590978"),
					new BigDecimal("-0.00002901664775368608454536093263140271210486528355244763337706390646279434621122"),
					10.0, // until 30
					4,
					20),
			new PointOfInterest(
					"Chaotic Spirals",
					new BigDecimal("-0.2634547678695909194066896880514263352460531386654479061034372986938"),
					new BigDecimal("-0.002712500848707182758893848976049032980969922260445790785383665852890"),
					4.9, // until 19
					12,
					100),
			new PointOfInterest(
					"Hydra",
					new BigDecimal("-0.047296221989823492360680509805723234463608050671596296439830057098"),
					new BigDecimal("-0.66103581599868663245324926435494860068349798558883084891691182468"),
					10.0, // until 17
					7,
					50),
			new PointOfInterest(
					"Nautilus Swarm",
					new BigDecimal("0.74364388856444046004654780500276747265963221621866228900489806506969863376"),
					new BigDecimal("0.13182590425618977278061359608949824676900962922349361652463219715391690224"),
					9.8, // until 25
					1,
					10),
			new PointOfInterest(
					"Wheels of Fire",
					new BigDecimal("1.348297061455330911551814316377597957726489116406445444186894528900140472"),
					new BigDecimal("0.049008405244229690790778032639417178346556340335816072865655769089707512"),
					9.8, // until 23
					8,
					200),
			new PointOfInterest(
					"Endless Spirals",
					new BigDecimal("-0.327723250308055864649016619795523424768319176482519467731956421519920253447363086958917926666960824258120609388163651727300535678400149632928730434212900"),
					new BigDecimal("0.0371201060581503985963131260660658234700437982666285956535462383384890416047297529586437189049645110208865816021476356710788140673394527005494736015790"),
					9.0, // until 100
					30,
					20),
			new PointOfInterest(
					"Jelly Fish",
					new BigDecimal("1.62060149612909895334558638175468271327989345822610379886703941363672342"),
					new BigDecimal("0.00684632316877134945739460204744981575825547591137510013929931504873578"),
					10.0, // until 20
					3,
					30),
			new PointOfInterest(
					"Nested Spirals",
					new BigDecimal("-0.2634547678695909194010687726751533781415432231241472469588605765799937908732975296976"),
					new BigDecimal("-0.002712500848707182783444349630487751292325741805030146491993712187120378441317722930"),
					8.0,
					6,
					40),
			new PointOfInterest(
					"Thorns",
					new BigDecimal("0.6156881882771651368740954356343166824243905327338704334095602362"),
					new BigDecimal("0.67490040735939139935516524107132991514169897692181761211725061908"),
					8.4, // until 16
					1,
					10),
			new PointOfInterest(
					"Deep Zoom 1",
					new BigDecimal("1.6287436846258729580610678388260250762812777365112032355154904837527664983515542212689174493"),
					new BigDecimal("0.03321567535450049497283969147810438048212502070056105912951434817286429422341078823068902338"),
					10.0, // until 88
					1,
					10),
			new PointOfInterest(
					"Deep Zoom 2",
					new BigDecimal("0.17397289514986169639824541981576261104970879121077891755416819503195995848966918072069375867181800169256166"),
					new BigDecimal("1.08734539158921551497256513686663305175779943810201126120006971717150493897765399921888763638520188629833582"),
					4.0, // until 57
					1,
					10),
			new PointOfInterest(
					"Classic Deep",
					new BigDecimal("1.740062382579339905220844167065825638296641720436171866879862418461182919644153056054840718339483225743450008259172138785492983677893366503417299549623738838303346465461290768441055486136870719850559269507357211790243666940134793753068611574745943820712885258222629105433648695946003865"),
					new BigDecimal("0.0281753397792110489924115211443195096875390767429906085704013095958801743240920186385400814658560553615695084486774077000669037710191665338060418999324320867147028768983704831316527873719459264592084600433150333362859318102017032958074799966721030307082150171994798478089798638258639934"),
					5.0,
					1,
					5),
			new PointOfInterest(
					"Close to the Tip",
					new BigDecimal("1.999774075531510062196466924922751584703084668836849819676693632045811372575383342685205522372465313107518959003812"),
					new BigDecimal("3.375402489828553121033532529759462615490836723815772824E-60"),
					5.0, // until 18
					1,
					5),
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
		
		ComboBox<PointOfInterest> pointsOfInterestComboBox = new ComboBox<>(FXCollections.observableList(Arrays.asList(POINTS_OF_INTEREST)));
		box.getChildren().add(pointsOfInterestComboBox);
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
		case IsoLine:
			palette = new CyclingPalette(Color.WHITE, steps, Color.LIGHTGREY, Color.GRAY, Color.DARKGRAY, Color.BLACK);
			break;
		case Fire:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.RED, Color.YELLOW, Color.DARKRED, Color.ORANGE, Color.gray(0.1)), steps));
			break;
		case Forest:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.GREENYELLOW, Color.GREEN, Color.DARKGREEN, Color.LIGHTGREEN, Color.gray(0.1)), steps));
			break;
		case Ice:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.BLUE, Color.LIGHTBLUE, Color.DARKBLUE, Color.CYAN, Color.gray(0.1)), steps));
			break;
		case StarryNight:
			palette = new CachingPalette(new InterpolatingPalette(new CyclingPalette(Color.WHITE, Color.gray(0.1)), steps));
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
