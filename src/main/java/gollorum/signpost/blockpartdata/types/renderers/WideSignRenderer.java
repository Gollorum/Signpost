package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.rendering.ModelRegistry;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.modelGeneration.SignModel;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.vector.Vector3f;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class WideSignRenderer extends SignRenderer<SmallWideSignBlockPart> {

	private static final float TEXT_OFFSET_RIGHT = 7f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT = 11f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	@Override
	protected IBakedModel makeBakedModel(SmallWideSignBlockPart sign) {
		return ModelRegistry.WideBakedSign.makeModel(sign);
	}

	@Override
	protected IBakedModel makeBakedOverlayModel(SmallWideSignBlockPart sign, Overlay overlay) {
		return ModelRegistry.WideBakedSign.makeOverlayModel(sign, overlay);
	}

	@Override
	protected SignModel makeModel(SmallWideSignBlockPart sign) {
		return ModelRegistry.WideSign.makeModel(sign);
	}

	@Override
	protected SignModel makeOverlayModel(SmallWideSignBlockPart sign, Overlay overlay) {
		return ModelRegistry.WideSign.makeOverlayModel(sign, overlay);
	}

	@Override
	protected void renderText(SmallWideSignBlockPart sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights) {
		RenderingUtil.wrapInMatrixEntry(matrix, () -> {
			matrix.mulPose(Vector3f.ZP.rotationDegrees(180));
			float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
			float MAX_WIDTH_FRAC = fontRenderer.width(sign.getText()) * scale / MAXIMUM_TEXT_WIDTH;
			scale /= Math.max(1, MAX_WIDTH_FRAC);
			float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
			matrix.translate(
				sign.isFlipped() ? offset - fontRenderer.width(sign.getText()) * scale : -offset,
				-scale * 4 * TEXT_RATIO,
				-3.005 * VoxelSize);
			matrix.scale(scale, scale * TEXT_RATIO, scale);
			fontRenderer.drawInBatch(sign.getText(), 0, 0, sign.getColor(), false, matrix.last().pose(), buffer, false, 0, combinedLights);
		});
	}

}
