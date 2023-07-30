package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.rendering.ModelRegistry;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.modelGeneration.SignModel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;

import java.util.Optional;
import java.util.Random;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class WideSignRenderer extends SignRenderer<SmallWideSignBlockPart> {

	private static final float TEXT_OFFSET_RIGHT = 7f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT = 11f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	@Override
	protected BakedModel makeBakedModel(SmallWideSignBlockPart sign) {
		return ModelRegistry.WideBakedSign.makeModel(sign);
	}

	@Override
	protected BakedModel makeBakedOverlayModel(SmallWideSignBlockPart sign, Overlay overlay) {
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
	protected void renderText(SmallWideSignBlockPart sign, PoseStack matrix, Font fontRenderer, MultiBufferSource buffer, int combinedLights) {
		RenderingUtil.wrapInMatrixEntry(matrix, () -> {
			var text = sign.getText().get();
			if(sign.isMarkedForGeneration()) {
				var overrideName = WaystoneLibrary.getInstance().getAllWaystoneNames(true)
						.flatMap(s -> !s.isEmpty()
							? (s.size() == 1
								? s.stream()
								: s.stream().skip(new Random().nextInt(s.size() - 1))
							).findFirst()
							: Optional.empty());
				if(overrideName.isPresent()) text = overrideName.get();
			}
			matrix.mulPose(Vector3f.ZP.rotationDegrees(180));
			float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
			float MAX_WIDTH_FRAC = fontRenderer.width(text) * scale / MAXIMUM_TEXT_WIDTH;
			scale /= Math.max(1, MAX_WIDTH_FRAC);
			float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
			matrix.translate(
				sign.isFlipped() ? offset - fontRenderer.width(text) * scale : -offset,
				-scale * 4 * TEXT_RATIO,
				-3.005 * VoxelSize);
			matrix.scale(scale, scale * TEXT_RATIO, scale);
			fontRenderer.drawInBatch(text, 0, 0, sign.getColor(), false, matrix.last().pose(), buffer, false, 0, combinedLights);
		});
	}

}
