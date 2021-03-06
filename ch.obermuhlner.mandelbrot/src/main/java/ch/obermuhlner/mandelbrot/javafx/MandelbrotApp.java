package ch.obermuhlner.mandelbrot.javafx;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import ch.obermuhlner.mandelbrot.movie.MandelbrotMovie;
import ch.obermuhlner.mandelbrot.movie.MovieStep;
import ch.obermuhlner.mandelbrot.palette.Color;
import ch.obermuhlner.mandelbrot.palette.Palette;
import ch.obermuhlner.mandelbrot.palette.PaletteFactory;
import ch.obermuhlner.mandelbrot.palette.PaletteType;
import ch.obermuhlner.mandelbrot.poi.PointOfInterest;
import ch.obermuhlner.mandelbrot.poi.StandardPointsOfInterest;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class MandelbrotApp extends Application {

	private static final double KEY_TRANSLATE_FACTOR = 0.1;
	private static final double KEY_ZOOM_STEP = 0.1;
	private static final double SCROLL_ZOOM_STEP = 0.5;

	private static final int IMAGE_SIZE = 256+128+64;
	
	private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("##0");
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("##0.000");
	private static final DecimalFormat DOUBLE_8DIGITS_FORMAT = new DecimalFormat("##0.00000000");
	
	private static final javafx.scene.paint.Color WHITE_90 = new javafx.scene.paint.Color(1.0, 1.0, 1.0, 0.9);
	private static final javafx.scene.paint.Color WHITE_20 = new javafx.scene.paint.Color(1.0, 1.0, 1.0, 0.2);

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
	private BooleanProperty autoMaxIterationProperty = new SimpleBooleanProperty(true); 
	private IntegerProperty maxIterationProperty = new SimpleIntegerProperty(1000);
	private IntegerProperty maxIterationAtZeroZoomLevelProperty = new SimpleIntegerProperty(1000);
	private IntegerProperty maxIterationPerZoomLevelProperty = new SimpleIntegerProperty(1000);
	
	private ObjectProperty<PaletteType> paletteTypeProperty = new SimpleObjectProperty<PaletteType>(PaletteType.RandomColor);
	private IntegerProperty paletteSeedProperty = new SimpleIntegerProperty(14);
	private IntegerProperty paletteStepProperty = new SimpleIntegerProperty(20);
	
	private BooleanProperty crosshairProperty = new SimpleBooleanProperty(true); 
	private BooleanProperty gridProperty = new SimpleBooleanProperty(false); 

	private IntegerProperty snapshotWidthProperty = new SimpleIntegerProperty(1920);
	private IntegerProperty snapshotHeightProperty = new SimpleIntegerProperty(1200);

	private IntegerProperty movieWidthProperty = new SimpleIntegerProperty(1280);
	private IntegerProperty movieHeightProperty = new SimpleIntegerProperty(720);
	private ListProperty<MovieStep> movieStepsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

	private PaletteFactory paletteFactory = new PaletteFactory();
	private Palette palette;

	private Canvas mandelbrotCanvas;
	private WritableImage image = new WritableImage(IMAGE_SIZE, IMAGE_SIZE);

	private final Path homeDirectory = homeDirectory();

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
		
		Node toolbar = createToolbar(primaryStage);
		borderPane.setTop(toolbar);

		Node editor = createEditor();
		borderPane.setRight(editor);

		mandelbrotCanvas = createMandelbrotCanvas();
		borderPane.setCenter(mandelbrotCanvas);

		TabPane tabPane = new TabPane();
		borderPane.setBottom(tabPane);
		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

		Node snapshotEditor = createSnapshotEditor();
		tabPane.getTabs().add(new Tab("Snapshots", snapshotEditor));

		Node movieEditor = createMovieEditor();
		tabPane.getTabs().add(new Tab("Movie", movieEditor));

		primaryStage.setScene(scene);
		primaryStage.show();
		
		mandelbrotCanvas.requestFocus();
		
		primaryStage.setOnCloseRequest(event -> {
			if (backgroundSnapshotRenderer.getPendingSnapshotRequestCount() > 0) {
				Alert alert = new Alert(
						AlertType.CONFIRMATION,
						"You still have pending snapshots to be calculated.\n\n" +
						"Do you want to calculate all pending snapshots in the background after closing the application?\n",
						ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
				Optional<ButtonType> optionalAlertResult = alert.showAndWait();
				if (optionalAlertResult.isPresent()) {
					ButtonType alertResult = optionalAlertResult.get();
					if (alertResult == ButtonType.YES) {
						// do nothing
					} else if (alertResult == ButtonType.NO) {
						backgroundSnapshotRenderer.cancelAllSnapshotRequestsAndStopRunning();
					} else if (alertResult == ButtonType.CANCEL) {
						event.consume();
					} else {
						throw new IllegalArgumentException("Unexpected alert result: " + alertResult);
					}
				}
			}
		});
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

	private Node createToolbar(Stage stage) {
		HBox box = new HBox(2);

		Button openButton = new Button("Open...");
		box.getChildren().add(openButton);
		openButton.setOnAction(event -> {
			openMandelbrotFile(stage);
		});
		
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
			setPointOfInterest(pointOfInterest);
		});
				
		return box;
	}
	
	private void openMandelbrotFile(Stage stage) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(homeDirectory.toFile());
		fileChooser.setTitle("Open Mandelbrot");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Mandelbrot", "*.mandelbrot"));
		File file = fileChooser.showOpenDialog(stage);
		
		if (file != null) {
			try {
				setPointOfInterest(PointOfInterest.load(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void setPointOfInterest(PointOfInterest pointOfInterest) {
		xCenterProperty.set(pointOfInterest.x);
		yCenterProperty.set(pointOfInterest.y);
		zoomProperty.set(pointOfInterest.zoom);
		paletteTypeProperty.set(pointOfInterest.paletteType);
		paletteSeedProperty.set(pointOfInterest.paletteSeed);
		paletteStepProperty.set(pointOfInterest.paletteStep);
		if (pointOfInterest.maxIterationsLinear == 0) {
			autoMaxIterationProperty.set(false);
			maxIterationProperty.set(pointOfInterest.maxIterationsConst);			
			maxIterationAtZeroZoomLevelProperty.set(1000);
		} else {
			autoMaxIterationProperty.set(true);
			maxIterationAtZeroZoomLevelProperty.set(pointOfInterest.maxIterationsConst);
			maxIterationPerZoomLevelProperty.set(pointOfInterest.maxIterationsLinear);
		}
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

		gridPane.add(new Label("Max Iterations:"), 0, rowIndex);
		TextField maxIterationTextField = new TextField();
		gridPane.add(maxIterationTextField, 1, rowIndex);
		maxIterationTextField.disableProperty().bind(autoMaxIterationProperty);
		Bindings.bindBidirectional(maxIterationTextField.textProperty(), maxIterationProperty, INTEGER_FORMAT);
		CheckBox autoMaxIterationCheckBox = new CheckBox("Auto");
		Bindings.bindBidirectional(autoMaxIterationCheckBox.selectedProperty(), autoMaxIterationProperty);
		gridPane.add(autoMaxIterationCheckBox, 2, rowIndex);
		updateAutoMaxIterationFormula(autoMaxIterationProperty.get());
		autoMaxIterationProperty.addListener((observable, oldValue, newAutoMaxIteration) -> {
			updateAutoMaxIterationFormula(newAutoMaxIteration);
		});
		rowIndex++;

		gridPane.add(new Label("Max Iterations/Zoom:"), 0, rowIndex);
		TextField maxIterationPerZoomLevelTextField = new TextField();
		maxIterationPerZoomLevelTextField.disableProperty().bind(autoMaxIterationProperty.not());
		gridPane.add(maxIterationPerZoomLevelTextField, 1, rowIndex);
		Bindings.bindBidirectional(maxIterationPerZoomLevelTextField.textProperty(), maxIterationPerZoomLevelProperty, INTEGER_FORMAT);
		rowIndex++;

		gridPane.add(new Label("Color Palette:"), 0, rowIndex);
		ComboBox<PaletteType> paletteTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(PaletteType.values()));
		gridPane.add(paletteTypeComboBox, 1, rowIndex);
		Bindings.bindBidirectional(paletteTypeComboBox.valueProperty(), paletteTypeProperty);
		rowIndex++;
		
		gridPane.add(new Label("Color Scheme:"), 0, rowIndex);
		TextField paletteSeedTextField = new TextField();
		gridPane.add(paletteSeedTextField, 1, rowIndex);
		Bindings.bindBidirectional(paletteSeedTextField.textProperty(), paletteSeedProperty, INTEGER_FORMAT);
		Button paletteSeedDownButton = new Button("<");
		paletteSeedDownButton.setOnAction(event -> {
			if (paletteSeedProperty.get() > 0) {
				paletteSeedProperty.set(paletteSeedProperty.get() - 1);
			}
		});
		Button paletteSeedUpButton = new Button(">");
		paletteSeedUpButton.setOnAction(event -> {
			paletteSeedProperty.set(paletteSeedProperty.get() + 1);
		});
		HBox paletteSeedUpDownBox = new HBox(paletteSeedDownButton, paletteSeedUpButton);
		gridPane.add(paletteSeedUpDownBox, 2, rowIndex);
		rowIndex++;

		gridPane.add(new Label("Color Step:"), 0, rowIndex);
		TextField paletteStepTextField = new TextField();
		gridPane.add(paletteStepTextField, 1, rowIndex);
		Bindings.bindBidirectional(paletteStepTextField.textProperty(), paletteStepProperty, INTEGER_FORMAT);
		rowIndex++;
		
		return box;
	}

	private void updateAutoMaxIterationFormula(boolean autoMaxIteration) {
		if (autoMaxIteration) {
			maxIterationProperty.bind(maxIterationPerZoomLevelProperty.multiply(zoomProperty).add(maxIterationAtZeroZoomLevelProperty.get()));
		} else {
			maxIterationProperty.unbind();
		}
	}
	
	private Node createSnapshotEditor() {
		VBox vBox = new VBox(4);

		{
			HBox hBox = new HBox(4);
			vBox.getChildren().add(hBox);
			
			TextField widthTextField = new TextField();
			widthTextField.setPrefWidth(60);
			hBox.getChildren().add(widthTextField);
			Bindings.bindBidirectional(widthTextField.textProperty(), snapshotWidthProperty, INTEGER_FORMAT);

			hBox.getChildren().add(new Label("x"));

			TextField heightTextField = new TextField();
			heightTextField.setPrefWidth(60);
			hBox.getChildren().add(heightTextField);
			Bindings.bindBidirectional(heightTextField.textProperty(), snapshotHeightProperty, INTEGER_FORMAT);

			Button snapshotButton = new Button("Snapshot");
			hBox.getChildren().add(snapshotButton);
			snapshotButton.setOnAction(event -> {
				String basename = "mandelbrot_" + LocalDateTime.now().toString().replace(':', '_');

				String mandelbrotFilename = basename + ".mandelbrot";
				int maxIterationsConst;
				int maxIterationsLinear;
				if (autoMaxIterationProperty.get()) {
					maxIterationsConst = maxIterationAtZeroZoomLevelProperty.get();
					maxIterationsLinear = maxIterationPerZoomLevelProperty.get();
				} else {
					maxIterationsConst = maxIterationProperty.get();
					maxIterationsLinear = 0;
				}
				PointOfInterest pointOfInterest = new PointOfInterest(
						basename,
						xCenterProperty.get(),
						yCenterProperty.get(),
						zoomProperty.get(),
						paletteTypeProperty.get(),
						paletteSeedProperty.get(),
						paletteStepProperty.get(),
						maxIterationsConst,
						maxIterationsLinear);
				try {
					pointOfInterest.save(homeDirectory.resolve(mandelbrotFilename).toFile());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				String snapshotFilename = basename + ".png";
				DrawRequest drawRequest = new DrawRequest(xCenterProperty.get(), yCenterProperty.get(), zoomProperty.get(), maxIterationProperty.get());
				int width = snapshotWidthProperty.get();
				int height = snapshotHeightProperty.get();
				backgroundSnapshotRenderer.addSnapshotRequest(new SnapshotRequest(drawRequest, palette, width, height, homeDirectory.resolve(snapshotFilename).toFile()));
			});
		}

		{
			TableView<SnapshotRequest> snapshotTableView = new TableView<>(backgroundSnapshotRenderer.getSnapshotRequests());
			vBox.getChildren().add(snapshotTableView);
			snapshotTableView.setPrefHeight(100);
			snapshotTableView.setRowFactory(new Callback<TableView<SnapshotRequest>, TableRow<SnapshotRequest>>() {
				@Override
				public TableRow<SnapshotRequest> call(TableView<SnapshotRequest> param) {
					TableRow<SnapshotRequest> tableRow = new TableRow<>();
					tableRow.setOnMouseClicked(event -> {
						if (event.getClickCount() == 2) {
							File file = tableRow.getItem().file;
							if (file.exists()) {
								try {
									Desktop.getDesktop().open(file);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					});
					MenuItem removeMenuItem = new MenuItem("Remove");
					removeMenuItem.setOnAction(event -> {
						backgroundSnapshotRenderer.removeSnapshotRequest(tableRow.getItem());
					});
					tableRow.setContextMenu(new ContextMenu(
								removeMenuItem
							));
					return tableRow;
				}
			});
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
				return snapshotRequest.calculationTimeProperty();
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
		}

		return vBox;
	}

	private Node createMovieEditor() {
		BorderPane borderPane = new BorderPane();

		{
			HBox hBox = new HBox(4);
			borderPane.setTop(hBox);

			TextField widthTextField = new TextField();
			widthTextField.setPrefWidth(60);
			hBox.getChildren().add(widthTextField);
			Bindings.bindBidirectional(widthTextField.textProperty(), movieWidthProperty, INTEGER_FORMAT);

			hBox.getChildren().add(new Label("x"));

			TextField heightTextField = new TextField();
			heightTextField.setPrefWidth(60);
			hBox.getChildren().add(heightTextField);
			Bindings.bindBidirectional(heightTextField.textProperty(), movieHeightProperty, INTEGER_FORMAT);

			Button createMovieButton = new Button("Create Movie");
			hBox.getChildren().add(createMovieButton);
			createMovieButton.setOnAction(event -> {
				new Thread(() -> {
					createMovieButton.setDisable(true);
					try {
						String basename = "mandelbrot_" + LocalDateTime.now().toString().replace(':', '_');
						Path directory = homeDirectory.resolve(basename);
						MandelbrotMovie movie = new MandelbrotMovie();
						movie.createMovie(directory, movieStepsProperty.get());
					} finally {
						createMovieButton.setDisable(false);
					}
				}).start();
			});
		}

		TableView<MovieStep> movieStepsTableView = new TableView<>(movieStepsProperty);
		{
			borderPane.setCenter(movieStepsTableView);
			movieStepsTableView.setPrefHeight(100);
			movieStepsTableView.setRowFactory(new Callback<TableView<MovieStep>, TableRow<MovieStep>>() {
				@Override
				public TableRow<MovieStep> call(TableView<MovieStep> param) {
					TableRow<MovieStep> tableRow = new TableRow<>();

					return tableRow;
				}
			});
			addTableColumn(movieStepsTableView, "X", 100, movieStep -> {
				return new ReadOnlyStringWrapper(DOUBLE_8DIGITS_FORMAT.format(movieStep.x));
			});
			addTableColumn(movieStepsTableView, "Y", 100, movieStep -> {
				return new ReadOnlyStringWrapper(DOUBLE_8DIGITS_FORMAT.format(movieStep.y));
			});
			addTableColumn(movieStepsTableView, "Zoom", 60, movieStep -> {
				return new ReadOnlyStringWrapper(DOUBLE_FORMAT.format(movieStep.zoom));
			});
		}

		{
			VBox vBox = new VBox(4);
			borderPane.setRight(vBox);

			Button addStepButton = new Button("Add");
			vBox.getChildren().add(addStepButton);
			addStepButton.setOnAction(event -> {
				MovieStep movieStep = new MovieStep(
						xCenterProperty.get(),
						yCenterProperty.get(),
						BigDecimal.valueOf(zoomProperty.get()),
						palette);
				movieStepsProperty.add(movieStep);
			});

			Button removeStepButton = new Button("Remove");
			vBox.getChildren().add(removeStepButton);
			removeStepButton.setOnAction(event -> {
				MovieStep selectedMovieStep = movieStepsTableView.getSelectionModel().getSelectedItem();
				if (selectedMovieStep != null) {
					movieStepsProperty.remove(selectedMovieStep);
				}
			});

			Button upStepButton = new Button("Up");
			vBox.getChildren().add(upStepButton);
			upStepButton.setOnAction(event -> {
				int selectedIndex = movieStepsTableView.getSelectionModel().getSelectedIndex();
				if (selectedIndex > 0) {
					MovieStep movieStep = movieStepsProperty.remove(selectedIndex);
					movieStepsProperty.add(selectedIndex - 1, movieStep);
					movieStepsTableView.getSelectionModel().select(selectedIndex - 1);
				}
			});

			Button downStepButton = new Button("Down");
			vBox.getChildren().add(downStepButton);
			downStepButton.setOnAction(event -> {
				int selectedIndex = movieStepsTableView.getSelectionModel().getSelectedIndex();
				if (selectedIndex >= 0 && selectedIndex < movieStepsProperty.size() - 1) {
					MovieStep movieStep = movieStepsProperty.remove(selectedIndex);
					movieStepsProperty.add(selectedIndex + 1, movieStep);
					movieStepsTableView.getSelectionModel().select(selectedIndex + 1);
				}
			});

			removeStepButton.setDisable(true);
			upStepButton.setDisable(true);
			downStepButton.setDisable(true);
			movieStepsTableView.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
				int selectedIndex = newValue.intValue();
				if (selectedIndex < 0) {
					removeStepButton.setDisable(true);
					upStepButton.setDisable(true);
					downStepButton.setDisable(true);
				} else {
					removeStepButton.setDisable(false);
					upStepButton.setDisable(selectedIndex == 0);
					downStepButton.setDisable(selectedIndex == movieStepsProperty.size() - 1);
				}
			});
		}

		return borderPane;
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

		palette = paletteFactory.createPalette(paletteTypeProperty.get(), seed, steps);

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
		backgroundProgressiveRenderer.triggerDraw(new DrawRequest(xCenterProperty.get(), yCenterProperty.get(), zoomProperty.get(), maxIterationProperty.get()));
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
							pixelWriter.setColor(px, py, ColorUtil.toJavafxColor(color));
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
							pixelWriter.setColor(px, py, ColorUtil.toJavafxColor(color));
						}
					}
				}
				
				y0 = y0.add(blockStepY, mc);
			}
		});
	}

	private static Path homeDirectory() {
		Path path = Paths.get(System.getProperty("user.home", "."), "Mandelbrot");
		path.toFile().mkdirs();
		return path;
	}

	public static void main(String[] args) {
		launch(args);
	}

}
