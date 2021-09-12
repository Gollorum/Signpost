package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.rendering.ModelRegistry;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.MathUtils;
import gollorum.signpost.utils.modelGeneration.SignModel;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.vector.Vector3f;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class ShortSignRenderer extends SignRenderer<SmallShortSignBlockPart> {

	private static final float TEXT_OFFSET_RIGHT = -3f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT = 13.5f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	@Override
	protected IBakedModel makeBakedModel(SmallShortSignBlockPart sign) {
		return ModelRegistry.ShortBakedSign.makeModel(sign);
	}

	@Override
	protected IBakedModel makeBakedOverlayModel(SmallShortSignBlockPart sign, Overlay overlay) {
		return ModelRegistry.ShortBakedSign.makeOverlayModel(sign, overlay);
	}

	@Override
	protected SignModel makeModel(SmallShortSignBlockPart sign) {
		return ModelRegistry.ShortSign.makeModel(sign);
	}

	@Override
	protected SignModel makeOverlayModel(SmallShortSignBlockPart sign, Overlay overlay) {
		return ModelRegistry.ShortSign.makeOverlayModel(sign, overlay);
	}

	@Override
	protected void renderText(SmallShortSignBlockPart sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights) {
		renderText(true, sign, matrix, fontRenderer, buffer, combinedLights);
		renderText(false, sign, matrix, fontRenderer, buffer, combinedLights);
	}

	private void renderText(boolean isFlipped, SmallShortSignBlockPart sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights) {
		RenderingUtil.wrapInMatrixEntry(matrix, () -> {
			matrix.mulPose(Vector3f.ZP.rotationDegrees(180));
			float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
			float MAX_WIDTH_FRAC = fontRenderer.width(sign.getText()) * scale / MAXIMUM_TEXT_WIDTH;
			scale /= Math.max(1, MAX_WIDTH_FRAC);
			boolean flipped = isFlipped ^ sign.isFlipped();
			if(isFlipped) matrix.mulPose(Vector3f.YP.rotation((float) Math.PI));
			float offset = MathUtils.lerp(TEXT_OFFSET_RIGHT, (TEXT_OFFSET_RIGHT - TEXT_OFFSET_LEFT) / 2f, 1 - Math.min(1, MAX_WIDTH_FRAC));
			matrix.translate(
				flipped ? offset - fontRenderer.width(sign.getText()) * scale : -offset,
				-scale * 4 * TEXT_RATIO,
				-0.505 * VoxelSize);
			matrix.scale(scale, scale * TEXT_RATIO, scale);
			fontRenderer.drawInBatch(sign.getText(), 0, 0, sign.getColor(), false, matrix.last().pose(), buffer, false, 0, combinedLights);
		});
	}

}
