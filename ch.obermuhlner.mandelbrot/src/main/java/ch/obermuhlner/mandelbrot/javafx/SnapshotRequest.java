package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;

import ch.obermuhlner.mandelbrot.palette.Palette;
import javafx.beans.property.*;

public class SnapshotRequest implements Progress {
	private static final long MILLISECONDS_PER_HOUR = 60 * 60 * 1000;
	private static final long MILLISECONDS_PER_MINUTE = 60 * 1000;
	private static final long MILLISECONDS_PER_SECOND = 1000;

	public final DrawRequest drawRequest;
	public final Palette palette;
	public final int width;
	public final int height;
	public final File file;
	
	private double totalProgress;
	private double currentProgress;
	private final DoubleProperty progressProperty = new SimpleDoubleProperty();
	private final StringProperty calculationTimeProperty = new SimpleStringProperty();
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

	public StringProperty calculationTimeProperty() {
		return calculationTimeProperty;
	}

	public void setCalculationMillis(long millis) {
		long remainingMillis = millis;

		long hours = remainingMillis / MILLISECONDS_PER_HOUR;
		remainingMillis -= hours * MILLISECONDS_PER_HOUR;

		long minutes = remainingMillis / MILLISECONDS_PER_MINUTE;
		remainingMillis -= minutes * MILLISECONDS_PER_MINUTE;

		long seconds = remainingMillis / MILLISECONDS_PER_SECOND;
		remainingMillis -= seconds * MILLISECONDS_PER_SECOND;

		StringBuilder textBuilder = new StringBuilder();
		if (hours > 0) {
			textBuilder.append(hours);
			textBuilder.append("h ");
		}
		if (minutes > 0) {
			textBuilder.append(minutes);
			textBuilder.append("m ");
		}
		if (seconds > 0) {
			textBuilder.append(seconds);
			textBuilder.append("s ");
		}
		if (remainingMillis > 0) {
			textBuilder.append(remainingMillis);
			textBuilder.append("ms ");
		}

		String text = textBuilder.toString();

		if (text.equals("")) {
			text = "< 1 ms";
		}

		calculationTimeProperty.set(text);
	}
	
	public ObjectProperty<SnapshotStatus> snapshotStatusProperty() {
		return snapshotStatusProperty;
	}
}
