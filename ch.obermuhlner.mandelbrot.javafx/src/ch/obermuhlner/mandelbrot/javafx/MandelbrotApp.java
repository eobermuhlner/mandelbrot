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

	private static final int MAX_ITERATION = 1500;

	private static final BigDecimal BIGDECIMAL_THRESHOLD = new BigDecimal("0.00000000002");

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

	private static final StringConverter<BigDecimal> BIGDECIMAL_STRING_CONVERTER = new StringConverter<BigDecimal>() {
		@Override
		public String toString(BigDecimal object) {
			return object.toString();
		}

		@Override
		public BigDecimal fromString(String string) {
			return new BigDecimal(string);
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

	private static final BlockRenderInfo[] BLOCK_RENDER_INFOS_1x1 = {
			new BlockRenderInfo(1, 0, 0, 1),
	};
	
	private static final BlockRenderInfo[] BLOCK_RENDER_INFOS_4x4 = {
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
	
	private static final BlockRenderInfo[] BLOCK_RENDER_INFOS_8x8 = {
			// 8
			new BlockRenderInfo(8, 0, 0, 8),
			
			// 4
			new BlockRenderInfo(8, 4, 4, 4),
			new BlockRenderInfo(8, 0, 4, 4),
			new BlockRenderInfo(8, 4, 0, 4),

			// 2
			new BlockRenderInfo(8, 2, 2, 2),
			new BlockRenderInfo(8, 0, 2, 2),
			new BlockRenderInfo(8, 2, 0, 2),	
			new BlockRenderInfo(8, 2, 6, 2),	
			new BlockRenderInfo(8, 2, 4, 2),	
			new BlockRenderInfo(8, 0, 6, 2),	
			new BlockRenderInfo(8, 6, 2, 2),	
			new BlockRenderInfo(8, 6, 0, 2),	
			new BlockRenderInfo(8, 4, 2, 2),	
			new BlockRenderInfo(8, 6, 6, 2),	
			new BlockRenderInfo(8, 4, 6, 2),	
			new BlockRenderInfo(8, 6, 4, 2),

			// missing
			new BlockRenderInfo(8, 3, 0, 1),	
			new BlockRenderInfo(8, 5, 0, 1),	
			new BlockRenderInfo(8, 4, 1, 1),	
			new BlockRenderInfo(8, 5, 1, 1),	
			new BlockRenderInfo(8, 1, 4, 1),	
			new BlockRenderInfo(8, 5, 4, 1),	
			new BlockRenderInfo(8, 0, 5, 1),	
			new BlockRenderInfo(8, 1, 5, 1),	
			new BlockRenderInfo(8, 4, 5, 1),	
			new BlockRenderInfo(8, 5, 5, 1),	
			new BlockRenderInfo(8, 1, 1, 1),	
			new BlockRenderInfo(8, 0, 1, 1),	
			new BlockRenderInfo(8, 1, 0, 1),	
			new BlockRenderInfo(8, 3, 3, 1),	
			new BlockRenderInfo(8, 2, 3, 1),	
			new BlockRenderInfo(8, 3, 2, 1),	
			new BlockRenderInfo(8, 1, 3, 1),	
			new BlockRenderInfo(8, 0, 3, 1),	
			new BlockRenderInfo(8, 1, 2, 1),	
			new BlockRenderInfo(8, 3, 1, 1),	
			new BlockRenderInfo(8, 2, 1, 1),	
			new BlockRenderInfo(8, 3, 7, 1),	
			new BlockRenderInfo(8, 2, 7, 1),	
			new BlockRenderInfo(8, 3, 6, 1),	
			new BlockRenderInfo(8, 3, 5, 1),	
			new BlockRenderInfo(8, 2, 5, 1),	
			new BlockRenderInfo(8, 3, 4, 1),	
			new BlockRenderInfo(8, 1, 7, 1),	
			new BlockRenderInfo(8, 0, 7, 1),	
			new BlockRenderInfo(8, 1, 6, 1),	
			new BlockRenderInfo(8, 7, 3, 1),	
			new BlockRenderInfo(8, 6, 3, 1),	
			new BlockRenderInfo(8, 7, 2, 1),	
			new BlockRenderInfo(8, 7, 1, 1),
			new BlockRenderInfo(8, 6, 1, 1),
			new BlockRenderInfo(8, 7, 0, 1),
			new BlockRenderInfo(8, 5, 3, 1),	
			new BlockRenderInfo(8, 4, 3, 1),	
			new BlockRenderInfo(8, 5, 2, 1),	
			new BlockRenderInfo(8, 7, 7, 1),
			new BlockRenderInfo(8, 6, 7, 1),
			new BlockRenderInfo(8, 7, 6, 1),
			new BlockRenderInfo(8, 5, 7, 1),
			new BlockRenderInfo(8, 4, 7, 1),
			new BlockRenderInfo(8, 5, 6, 1),
			new BlockRenderInfo(8, 7, 5, 1),
			new BlockRenderInfo(8, 6, 5, 1),
			new BlockRenderInfo(8, 7, 4, 1),
	};

	private static final BlockRenderInfo[] BLOCK_RENDER_INFOS_16x16 = {
			//16
			new BlockRenderInfo(16, 0, 0, 16),
			
			//8
			new BlockRenderInfo(16, 8, 8, 8),
			new BlockRenderInfo(16, 0, 8, 8),
			new BlockRenderInfo(16, 8, 0, 8),

			//4
			new BlockRenderInfo(16, 4, 4, 4),
			new BlockRenderInfo(16, 0, 4, 4),
			new BlockRenderInfo(16, 4, 0, 4),	
			new BlockRenderInfo(16, 4, 12, 4),	
			new BlockRenderInfo(16, 4, 8, 4),	
			new BlockRenderInfo(16, 0, 12, 4),	
			new BlockRenderInfo(16, 12, 4, 4),	
			new BlockRenderInfo(16, 12, 0, 4),	
			new BlockRenderInfo(16, 8, 4, 4),	
			new BlockRenderInfo(16, 12, 12, 4),	
			new BlockRenderInfo(16, 8, 12, 4),	
			new BlockRenderInfo(16, 12, 8, 4),
			
			// 2
			new BlockRenderInfo(16, 6, 0, 2),	
			new BlockRenderInfo(16, 10, 0, 2),	
			new BlockRenderInfo(16, 8, 2, 2),	
			new BlockRenderInfo(16, 10, 2, 2),	
			new BlockRenderInfo(16, 2, 8, 2),	
			new BlockRenderInfo(16, 10, 8, 2),	
			new BlockRenderInfo(16, 0, 10, 2),	
			new BlockRenderInfo(16, 2, 10, 2),	
			new BlockRenderInfo(16, 8, 10, 2),	
			new BlockRenderInfo(16, 10, 10, 2),
			new BlockRenderInfo(16, 2, 2, 2),	
			new BlockRenderInfo(16, 0, 2, 2),	
			new BlockRenderInfo(16, 2, 0, 2),	
			new BlockRenderInfo(16, 6, 6, 2),	
			new BlockRenderInfo(16, 4, 6, 2),	
			new BlockRenderInfo(16, 6, 4, 2),	
			new BlockRenderInfo(16, 2, 6, 2),	
			new BlockRenderInfo(16, 0, 6, 2),	
			new BlockRenderInfo(16, 2, 4, 2),	
			new BlockRenderInfo(16, 6, 2, 2),	
			new BlockRenderInfo(16, 4, 2, 2),	
			new BlockRenderInfo(16, 6, 14, 2),	
			new BlockRenderInfo(16, 4, 14, 2),	
			new BlockRenderInfo(16, 6, 12, 2),	
			new BlockRenderInfo(16, 6, 10, 2),	
			new BlockRenderInfo(16, 4, 10, 2),	
			new BlockRenderInfo(16, 6, 8, 2),	
			new BlockRenderInfo(16, 2, 14, 2),	
			new BlockRenderInfo(16, 0, 14, 2),	
			new BlockRenderInfo(16, 2, 12, 2),	
			new BlockRenderInfo(16, 14, 6, 2),	
			new BlockRenderInfo(16, 12, 6, 2),	
			new BlockRenderInfo(16, 14, 4, 2),	
			new BlockRenderInfo(16, 14, 2, 2),
			new BlockRenderInfo(16, 12, 2, 2),
			new BlockRenderInfo(16, 14, 0, 2),
			new BlockRenderInfo(16, 10, 6, 2),	
			new BlockRenderInfo(16, 8, 6, 2),	
			new BlockRenderInfo(16, 10, 4, 2),	
			new BlockRenderInfo(16, 14, 14, 2),
			new BlockRenderInfo(16, 12, 14, 2),
			new BlockRenderInfo(16, 14, 12, 2),
			new BlockRenderInfo(16, 10, 14, 2),
			new BlockRenderInfo(16, 8, 14, 2),
			new BlockRenderInfo(16, 10, 12, 2),
			new BlockRenderInfo(16, 14, 10, 2),
			new BlockRenderInfo(16, 12, 10, 2),
			new BlockRenderInfo(16, 14, 8, 2),

			// 1
			new BlockRenderInfo(16, 1, 0, 1),	
			new BlockRenderInfo(16, 5, 0, 1),	
			new BlockRenderInfo(16, 9, 0, 1),	
			new BlockRenderInfo(16, 13, 0, 1),	
			new BlockRenderInfo(16, 0, 1, 1),	
			new BlockRenderInfo(16, 1, 1, 1),	
			new BlockRenderInfo(16, 4, 1, 1),	
			new BlockRenderInfo(16, 5, 1, 1),	
			new BlockRenderInfo(16, 8, 1, 1),	
			new BlockRenderInfo(16, 9, 1, 1),	
			new BlockRenderInfo(16, 12, 1, 1),	
			new BlockRenderInfo(16, 13, 1, 1),	
			new BlockRenderInfo(16, 4, 3, 1),	
			new BlockRenderInfo(16, 1, 4, 1),	
			new BlockRenderInfo(16, 5, 4, 1),	
			new BlockRenderInfo(16, 9, 5, 1),	
			new BlockRenderInfo(16, 13, 4, 1),	
			new BlockRenderInfo(16, 0, 5, 1),	
			new BlockRenderInfo(16, 1, 5, 1),	
			new BlockRenderInfo(16, 5, 5, 1),	
			new BlockRenderInfo(16, 8, 5, 1),	
			new BlockRenderInfo(16, 12, 5, 1),	
			new BlockRenderInfo(16, 13, 5, 1),	
			new BlockRenderInfo(16, 1, 8, 1),	
			new BlockRenderInfo(16, 3, 8, 1),	
			new BlockRenderInfo(16, 5, 8, 1),	
			new BlockRenderInfo(16, 13, 8, 1),	
			new BlockRenderInfo(16, 0, 9, 1),	
			new BlockRenderInfo(16, 1, 9, 1),	
			new BlockRenderInfo(16, 4, 9, 1),	
			new BlockRenderInfo(16, 5, 9, 1),	
			new BlockRenderInfo(16, 8, 9, 1),	
			new BlockRenderInfo(16, 9, 9, 1),	
			new BlockRenderInfo(16, 12, 9, 1),	
			new BlockRenderInfo(16, 13, 9, 1),	
			new BlockRenderInfo(16, 1, 12, 1),	
			new BlockRenderInfo(16, 5, 12, 1),	
			new BlockRenderInfo(16, 9, 12, 1),	
			new BlockRenderInfo(16, 13, 12, 1),	
			new BlockRenderInfo(16, 0, 13, 1),	
			new BlockRenderInfo(16, 1, 13, 1),	
			new BlockRenderInfo(16, 4, 13, 1),	
			new BlockRenderInfo(16, 5, 13, 1),	
			new BlockRenderInfo(16, 8, 13, 1),	
			new BlockRenderInfo(16, 9, 13, 1),	
			new BlockRenderInfo(16, 12, 13, 1),	
			new BlockRenderInfo(16, 13, 13, 1),	
			new BlockRenderInfo(16, 9, 4, 1),	
			new BlockRenderInfo(16, 7, 1, 1),	
			new BlockRenderInfo(16, 7, 0, 1),	
			new BlockRenderInfo(16, 6, 1, 1),	
			new BlockRenderInfo(16, 11, 1, 1),	
			new BlockRenderInfo(16, 10, 1, 1),	
			new BlockRenderInfo(16, 11, 0, 1),	
			new BlockRenderInfo(16, 9, 3, 1),	
			new BlockRenderInfo(16, 8, 3, 1),	
			new BlockRenderInfo(16, 9, 2, 1),	
			new BlockRenderInfo(16, 11, 3, 1),	
			new BlockRenderInfo(16, 10, 3, 1),	
			new BlockRenderInfo(16, 11, 2, 1),	
			new BlockRenderInfo(16, 3, 9, 1),	
			new BlockRenderInfo(16, 9, 8, 1),	
			new BlockRenderInfo(16, 2, 9, 1),	
			new BlockRenderInfo(16, 11, 9, 1),	
			new BlockRenderInfo(16, 10, 9, 1),	
			new BlockRenderInfo(16, 11, 8, 1),	
			new BlockRenderInfo(16, 1, 11, 1),	
			new BlockRenderInfo(16, 1, 10, 1),	
			new BlockRenderInfo(16, 0, 11, 1),	
			new BlockRenderInfo(16, 3, 11, 1),	
			new BlockRenderInfo(16, 2, 11, 1),	
			new BlockRenderInfo(16, 3, 10, 1),	
			new BlockRenderInfo(16, 9, 11, 1),	
			new BlockRenderInfo(16, 8, 11, 1),	
			new BlockRenderInfo(16, 9, 10, 1),	
			new BlockRenderInfo(16, 11, 11, 1),
			new BlockRenderInfo(16, 10, 11, 1),
			new BlockRenderInfo(16, 11, 10, 1),
			new BlockRenderInfo(16, 3, 3, 1),	
			new BlockRenderInfo(16, 2, 3, 1),	
			new BlockRenderInfo(16, 3, 2, 1),	
			new BlockRenderInfo(16, 1, 3, 1),	
			new BlockRenderInfo(16, 0, 3, 1),	
			new BlockRenderInfo(16, 1, 2, 1),	
			new BlockRenderInfo(16, 3, 1, 1),	
			new BlockRenderInfo(16, 2, 1, 1),	
			new BlockRenderInfo(16, 3, 0, 1),	
			new BlockRenderInfo(16, 7, 7, 1),	
			new BlockRenderInfo(16, 6, 7, 1),	
			new BlockRenderInfo(16, 7, 6, 1),	
			new BlockRenderInfo(16, 5, 7, 1),	
			new BlockRenderInfo(16, 4, 7, 1),	
			new BlockRenderInfo(16, 5, 6, 1),	
			new BlockRenderInfo(16, 7, 5, 1),	
			new BlockRenderInfo(16, 6, 5, 1),	
			new BlockRenderInfo(16, 7, 4, 1),	
			new BlockRenderInfo(16, 3, 7, 1),	
			new BlockRenderInfo(16, 2, 7, 1),	
			new BlockRenderInfo(16, 3, 6, 1),	
			new BlockRenderInfo(16, 1, 7, 1),	
			new BlockRenderInfo(16, 0, 7, 1),	
			new BlockRenderInfo(16, 1, 6, 1),	
			new BlockRenderInfo(16, 3, 5, 1),	
			new BlockRenderInfo(16, 2, 5, 1),	
			new BlockRenderInfo(16, 3, 4, 1),	
			new BlockRenderInfo(16, 7, 3, 1),	
			new BlockRenderInfo(16, 6, 3, 1),	
			new BlockRenderInfo(16, 7, 2, 1),	
			new BlockRenderInfo(16, 5, 3, 1),	
			new BlockRenderInfo(16, 4, 5, 1),	
			new BlockRenderInfo(16, 5, 2, 1),	
			new BlockRenderInfo(16, 7, 15, 1),	
			new BlockRenderInfo(16, 6, 15, 1),	
			new BlockRenderInfo(16, 7, 14, 1),	
			new BlockRenderInfo(16, 5, 15, 1),	
			new BlockRenderInfo(16, 4, 15, 1),	
			new BlockRenderInfo(16, 5, 14, 1),	
			new BlockRenderInfo(16, 7, 13, 1),	
			new BlockRenderInfo(16, 6, 13, 1),	
			new BlockRenderInfo(16, 7, 12, 1),	
			new BlockRenderInfo(16, 7, 11, 1),	
			new BlockRenderInfo(16, 6, 11, 1),	
			new BlockRenderInfo(16, 7, 10, 1),	
			new BlockRenderInfo(16, 5, 11, 1),	
			new BlockRenderInfo(16, 4, 11, 1),	
			new BlockRenderInfo(16, 5, 10, 1),	
			new BlockRenderInfo(16, 7, 9, 1),	
			new BlockRenderInfo(16, 6, 9, 1),	
			new BlockRenderInfo(16, 7, 8, 1),	
			new BlockRenderInfo(16, 3, 15, 1),	
			new BlockRenderInfo(16, 2, 15, 1),	
			new BlockRenderInfo(16, 3, 14, 1),	
			new BlockRenderInfo(16, 1, 15, 1),	
			new BlockRenderInfo(16, 0, 15, 1),	
			new BlockRenderInfo(16, 1, 14, 1),	
			new BlockRenderInfo(16, 3, 13, 1),	
			new BlockRenderInfo(16, 2, 13, 1),	
			new BlockRenderInfo(16, 3, 12, 1),	
			new BlockRenderInfo(16, 15, 7, 1),	
			new BlockRenderInfo(16, 14, 7, 1),	
			new BlockRenderInfo(16, 15, 6, 1),	
			new BlockRenderInfo(16, 13, 7, 1),	
			new BlockRenderInfo(16, 12, 7, 1),	
			new BlockRenderInfo(16, 13, 6, 1),	
			new BlockRenderInfo(16, 15, 5, 1),	
			new BlockRenderInfo(16, 14, 5, 1),	
			new BlockRenderInfo(16, 15, 4, 1),	
			new BlockRenderInfo(16, 15, 3, 1),
			new BlockRenderInfo(16, 14, 3, 1),
			new BlockRenderInfo(16, 15, 2, 1),
			new BlockRenderInfo(16, 13, 3, 1),
			new BlockRenderInfo(16, 12, 3, 1),
			new BlockRenderInfo(16, 13, 2, 1),
			new BlockRenderInfo(16, 15, 1, 1),
			new BlockRenderInfo(16, 14, 1, 1),
			new BlockRenderInfo(16, 15, 0, 1),
			new BlockRenderInfo(16, 11, 7, 1),	
			new BlockRenderInfo(16, 10, 7, 1),	
			new BlockRenderInfo(16, 11, 6, 1),	
			new BlockRenderInfo(16, 9, 7, 1),	
			new BlockRenderInfo(16, 8, 7, 1),	
			new BlockRenderInfo(16, 9, 6, 1),	
			new BlockRenderInfo(16, 11, 5, 1),	
			new BlockRenderInfo(16, 10, 5, 1),	
			new BlockRenderInfo(16, 11, 4, 1),	
			new BlockRenderInfo(16, 15, 15, 1),
			new BlockRenderInfo(16, 14, 15, 1),
			new BlockRenderInfo(16, 15, 14, 1),
			new BlockRenderInfo(16, 13, 15, 1),
			new BlockRenderInfo(16, 12, 15, 1),
			new BlockRenderInfo(16, 13, 14, 1),
			new BlockRenderInfo(16, 15, 13, 1),
			new BlockRenderInfo(16, 14, 13, 1),
			new BlockRenderInfo(16, 15, 12, 1),
			new BlockRenderInfo(16, 11, 15, 1),
			new BlockRenderInfo(16, 10, 15, 1),
			new BlockRenderInfo(16, 11, 14, 1),
			new BlockRenderInfo(16, 9, 15, 1),
			new BlockRenderInfo(16, 8, 15, 1),
			new BlockRenderInfo(16, 9, 14, 1),
			new BlockRenderInfo(16, 11, 13, 1),
			new BlockRenderInfo(16, 10, 13, 1),
			new BlockRenderInfo(16, 11, 12, 1),
			new BlockRenderInfo(16, 15, 11, 1),
			new BlockRenderInfo(16, 14, 11, 1),
			new BlockRenderInfo(16, 15, 10, 1),
			new BlockRenderInfo(16, 13, 11, 1),
			new BlockRenderInfo(16, 12, 11, 1),
			new BlockRenderInfo(16, 13, 10, 1),
			new BlockRenderInfo(16, 15, 9, 1),
			new BlockRenderInfo(16, 14, 9, 1),
			new BlockRenderInfo(16, 15, 8, 1),
	};

	private static final BlockRenderInfo[] BLOCK_RENDER_INFOS = BLOCK_RENDER_INFOS_16x16;
	
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
					int block = 0;
					while (block < BLOCK_RENDER_INFOS.length) {
						BlockRenderInfo blockRenderInfo = BLOCK_RENDER_INFOS[block];
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

	private ObjectProperty<BigDecimal> xCenterProperty = new SimpleObjectProperty<BigDecimal>(BigDecimal.ZERO);
	private ObjectProperty<BigDecimal> yCenterProperty = new SimpleObjectProperty<BigDecimal>(BigDecimal.ZERO);
	private DoubleProperty zoomProperty = new SimpleDoubleProperty(0.0);
	private ObjectProperty<BigDecimal> radiusProperty = new SimpleObjectProperty<BigDecimal>(BigDecimal.valueOf(2));
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
		GridPane gridPane = new GridPane();
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
		Bindings.bindBidirectional(radiusTextField.textProperty(), radiusProperty, BIGDECIMAL_STRING_CONVERTER);
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

		BigDecimal deltaX = BigDecimal.valueOf(deltaPixelX).divide(pixelWidth, MathContext.DECIMAL128).multiply(radiusProperty.get());
		BigDecimal deltaY = BigDecimal.valueOf(deltaPixelY).divide(pixelHeight, MathContext.DECIMAL128).multiply(radiusProperty.get());
		
		xCenterProperty.set(xCenterProperty.get().add(deltaX));
		yCenterProperty.set(yCenterProperty.get().add(deltaY));
		
		calculateAndDrawMandelbrot(canvas);
	}

	private void zoomScrollMandelbrot(Canvas canvas, double deltaPixelY) {
		double pixelHeight = canvas.getHeight();
	
		double deltaZ = deltaPixelY / pixelHeight * 2.0;
		
		radiusProperty.set(radiusProperty.get().multiply(BigDecimal.valueOf(1.0 + deltaZ)));

		calculateAndDrawMandelbrot(canvas);
	}

	private void zoomMandelbrot(Canvas canvas, double zoomFactor) {
		radiusProperty.set(radiusProperty.get().multiply(BigDecimal.valueOf(zoomFactor)));

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
		if (drawRequest.radius.compareTo(BIGDECIMAL_THRESHOLD) > 0) {
			calculateMandelbrotDouble(drawRequest, blockSize, blockPixelOffsetX, blockPixelOffsetY, pixelSize, maxIteration);
		} else {
			calculateMandelbrotBigDecimal(drawRequest, blockSize, blockPixelOffsetX, blockPixelOffsetY, pixelSize, maxIteration);
		}
	}
	
	private void calculateMandelbrotDouble(DrawRequest drawRequest, int blockSize, int blockPixelOffsetX, int blockPixelOffsetY, int pixelSize, int maxIteration) {
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double pixelWidth = image.getWidth();
		double pixelHeight = image.getHeight();

		double xRadius = drawRequest.radius.doubleValue();
		double yRadius = drawRequest.radius.doubleValue();
		double xCenter = drawRequest.x.doubleValue();
		double yCenter = drawRequest.y.doubleValue();
		
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

	private void calculateMandelbrotBigDecimal(DrawRequest drawRequest, int blockSize, int blockPixelOffsetX, int blockPixelOffsetY, int pixelSize, int maxIteration) {
		PixelWriter pixelWriter = image.getPixelWriter();
		
		double pixelWidth = image.getWidth();
		double pixelHeight = image.getHeight();
		
		MathContext mc = MathContext.DECIMAL128;

		BigDecimal xRadius = drawRequest.radius;
		BigDecimal yRadius = drawRequest.radius;
		BigDecimal xCenter = drawRequest.x;
		BigDecimal yCenter = drawRequest.y;
		
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
						if (pixelX + pixelOffsetX < pixelWidth && pixelY + pixelOffsetY < pixelHeight) {
							pixelWriter.setColor(pixelX + pixelOffsetX, pixelY + pixelOffsetY, color);
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
