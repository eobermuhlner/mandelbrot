package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;

import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SnapshotRequest implements Progress {
	public final DrawRequest drawRequest;
	public final Palette palette;
	public final int width;
	public final int height;
	public final File file;
	
	private double totalProgress;
	private double currentProgress;
	private final DoubleProperty progressProperty = new SimpleDoubleProperty();
	private final DoubleProperty calculationMillisProperty = new SimpleDoubleProperty();
	private final ObjectProperty<SnapshotStatus> snapshotStatusProperty = new SimpleObjectProperty<>(SnapshotStatus.Waiting);
	
	public SnapshotRequest(DrawRequest drawRequest, Palette palette, int width, int height, File file) {
		this.drawRequest = drawRequest;
		this.palette = palette;
		this.width = width;
		this.height = height;
		this.file = file;
		
		totalProgress = width * height;
	}
	
	@Override
	public void setTotalProgress(double totalProgress) {
		this.totalProgress = totalProgress;
	}
	
	@Override
	public double getProgress() {
		return progressProperty.get();
	}
	
	@Override
	public void incrementProgress(double progress) {
		currentProgress += progress;
		double relativeProgress = Math.min(1.0, currentProgress / totalProgress);
		progressProperty.set(relativeProgress);
	}
	
	public DoubleProperty progressProperty() {
		return progressProperty;
	}
	
	public DoubleProperty calculationMillisProperty() {
		return calculationMillisProperty;
	}
	
	public ObjectProperty<SnapshotStatus> snapshotStatusProperty() {
		return snapshotStatusProperty;
	}
}
