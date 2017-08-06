package ch.obermuhlner.mandelbrot.cli;

public class PrintMandelbrotMarkdownGallery {

	public static void main(String[] args) {
		for (String file : args) {
			System.out.println("[![](images/192x120/" + file + ")](images/1920x1200/" + file + "?raw=true)");
		}
	}
}
