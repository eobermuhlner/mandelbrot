package ch.obermuhlner.mandelbrot.javafx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DrawRequest {
	private static final BigDecimal BIGDECIMAL_THRESHOLD = new BigDecimal("0.00000000002");

	private static final int PROGRESSIVE_BLOCKS_DOUBLE = 4;
	private static final int PROGRESSIVE_BLOCKS_BIGDECIMAL = 256;

	private static final BlockRenderInfo[] PROGRESSIVE_RENDERINFO_DOUBLE = createBlockRenderInfos(PROGRESSIVE_BLOCKS_DOUBLE);
	private static final BlockRenderInfo[] PROGRESSIVE_RENDERINFO_BIGDECIMAL = createBlockRenderInfos(PROGRESSIVE_BLOCKS_BIGDECIMAL);

	public final BigDecimal x;
	public final BigDecimal y;
	public final double zoom;

	public DrawRequest(BigDecimal x, BigDecimal y, double zoom) {
		this.x = x;
		this.y = y;
		this.zoom = zoom;
	}

	public boolean isInsideDoublePrecision() {
		return getRadius().compareTo(BIGDECIMAL_THRESHOLD) > 0;
	}

	public BlockRenderInfo[] getProgressiveRenderInfo() {
		if (isInsideDoublePrecision()) {
			return PROGRESSIVE_RENDERINFO_DOUBLE;
		} else {
			return PROGRESSIVE_RENDERINFO_BIGDECIMAL;
		}
	}

	public int getPrecision() {
		return MandelbrotMath.getPrecision(zoom);
	}
	
	public BigDecimal getRadius() {
		return MandelbrotMath.getRadius(zoom);
	}

	public int getMaxIteration() {
		return MandelbrotMath.getMaxIteration(zoom);
	}
	
	@Override
	public String toString() {
		return "DrawRequest [x=" + x + ", y=" + y + ", zoom=" + zoom + "]";
	}

	private static BlockRenderInfo[] createBlockRenderInfos(int blockSize) {
		List<BlockRenderInfo> result = new ArrayList<>();
		
		result.add(new BlockRenderInfo(blockSize, 0, 0, blockSize));
		
		int pixelSize = blockSize / 2;
		while (pixelSize > 0) {
			List<BlockRenderInfo> children = new ArrayList<>();
			for (BlockRenderInfo blockRenderInfo : result) {
				children.add(new BlockRenderInfo(blockSize, blockRenderInfo.pixelOffsetX, blockRenderInfo.pixelOffsetY + pixelSize, pixelSize));
				children.add(new BlockRenderInfo(blockSize, blockRenderInfo.pixelOffsetX + pixelSize, blockRenderInfo.pixelOffsetY, pixelSize));
				children.add(new BlockRenderInfo(blockSize, blockRenderInfo.pixelOffsetX + pixelSize, blockRenderInfo.pixelOffsetY + pixelSize, pixelSize));
			}
			result.addAll(children);
			
			pixelSize /= 2;
		}
		
		return result.toArray(new BlockRenderInfo[0]);
	}
}
