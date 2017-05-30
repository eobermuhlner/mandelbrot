package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;

import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class SnapshotRequest implements Progress {
	public final DrawRequest drawRequest;
	public final Palette palette;
	public final File file;
	public final int width = 1200;
	public final int height = 900;
	private final DoubleProperty progressProperty = new SimpleDoubleProperty();
	
	public SnapshotRequest(DrawRequest drawRequest, Palette palette, File file) {
		this.drawRequest = drawRequest;
		this.palette = palette;
		this.file = file;
	}
	
	@Override
	public double getProgress() {
		return progressProperty.get();
	}
	
	@Override
	public void setProgress(double progress) {
		progressProperty.set(progress);
	}
	
	public DoubleProperty progressProperty() {
		return progressProperty;
	}
}
