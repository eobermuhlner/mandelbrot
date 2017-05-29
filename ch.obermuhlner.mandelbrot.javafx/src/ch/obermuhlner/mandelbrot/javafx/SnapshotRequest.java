package ch.obermuhlner.mandelbrot.javafx;

import java.io.File;

import ch.obermuhlner.mandelbrot.javafx.palette.Palette;

public class SnapshotRequest {
	public final DrawRequest drawRequest;
	public final Palette palette;
	public final File file;
	public final int width = 1200;
	public final int height = 900;
	
	public SnapshotRequest(DrawRequest drawRequest, Palette palette, File file) {
		this.drawRequest = drawRequest;
		this.palette = palette;
		this.file = file;
	}
}
