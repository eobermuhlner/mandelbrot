package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;

import ch.obermuhlner.mandelbrot.javafx.palette.Palette;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class SnapshotRequest implements Progress {
	public final DrawRequest drawRequest;
	public final Palette palette;
	public final File file;
	public final int width = 800;
	public final int height = 800;
	
	private double totalProgress = width * height;
	private double currentProgress;
	private final DoubleProperty progressProperty = new SimpleDoubleProperty();
	
	public SnapshotRequest(DrawRequest drawRequest, Palette palette, File file) {
		this.drawRequest = drawRequest;
		this.palette = palette;
		this.file = file;
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
}
