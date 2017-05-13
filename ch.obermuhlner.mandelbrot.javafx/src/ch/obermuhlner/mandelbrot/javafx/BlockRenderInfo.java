package ch.obermuhlner.mandelbrot.javafx;

class BlockRenderInfo {
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