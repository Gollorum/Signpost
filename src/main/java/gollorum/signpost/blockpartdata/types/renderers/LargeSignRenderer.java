package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.minecraft.rendering.ModelRegistry;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.modelGeneration.SignModel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.Random;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class LargeSignRenderer extends SignRenderer<LargeSignBlockPart> {

	private static final float TEXT_OFFSET_RIGHT = 7f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT_SHORT = 9f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT_LONG = 10f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH_SHORT = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT_SHORT;
	private static final float MAXIMUM_TEXT_WIDTH_LONG = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT_LONG;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	@Override
	protected BakedModel makeBakedModel(LargeSignBlockPart sign) {
		return ModelRegistry.LargeBakedSign.makeModel(sign);
	}

	@Override
	protected BakedModel makeBakedOverlayModel(LargeSignBlockPart sign, Overlay overlay) {
		return ModelRegistry.LargeBakedSign.makeOverlayModel(sign, overlay);
	}

	@Override
	protected SignModel makeModel(LargeSignBlockPart sign) {
		return ModelRegistry.LargeSign.makeModel(sign);
	}

	@Override
	protected SignModel makeOverlayModel(LargeSignBlockPart sign, Overlay overlay) {
		return ModelRegistry.LargeSign.makeOverlayModel(sign, overlay);
	}

	@Override
	public void renderText(LargeSignBlockPart sign, PoseStack matrix, Font fontRenderer, MultiBufferSource buffer, int combinedLights) {
		RenderingUtil.wrapInMatrixEntry(matrix, () -> {
			matrix.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3d(0, 0, 1))));
			matrix.translate(0, 3.5f * VoxelSize, -3.005 * VoxelSize);

			RenderingUtil.wrapInMatrixEntry(matrix, () -> render(sign, fontRenderer, sign.getText()[3].get(), matrix, buffer, combinedLights, false));
			matrix.translate(0, -7 / 3f * VoxelSize, 0);

			RenderingUtil.wrapInMatrixEntry(matrix, () -> render(sign, fontRenderer, sign.getText()[2].get(), matrix, buffer, combinedLights, false));
			matrix.translate(0, -7 / 3f * VoxelSize, 0);

			RenderingUtil.wrapInMatrixEntry(matrix, () -> render(sign, fontRenderer, sign.getText()[1].get(), matrix, buffer, combinedLights, false));
			matrix.translate(0, -7 / 3f * VoxelSize, 0);

			RenderingUtil.wrapInMatrixEntry(matrix, () -> render(sign, fontRenderer, sign.getText()[0].get(), matrix, buffer, combinedLights, false));
		});
	}

	private void render(LargeSignBlockPart sign, Font fontRenderer, String txt, PoseStack matrix, MultiBufferSource buffer, int combinedLights, boolean isLong) {
		RenderingUtil.wrapInMatrixEntry(matrix, () -> {
			var text = txt;
			if(sign.isMarkedForGeneration()) {
				var overrideName = WaystoneLibrary.getInstance().getAllWaystoneNames(true)
					.flatMap(s -> s.stream().skip(new Random().nextInt(s.size() - 1)).findFirst());
				if(overrideName.isPresent()) text = overrideName.get();
			}
			float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
			float MAX_WIDTH_FRAC = fontRenderer.width(text) * scale / (isLong ? MAXIMUM_TEXT_WIDTH_LONG : MAXIMUM_TEXT_WIDTH_SHORT);
			scale /= Math.max(1, MAX_WIDTH_FRAC);
			float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
			matrix.translate(
				sign.isFlipped() ? offset - fontRenderer.width(text) * scale : -offset,
				-scale * 4 * TEXT_RATIO,
				0
			);
			matrix.scale(scale, scale * TEXT_RATIO, scale);
			fontRenderer.drawInBatch(text, 0, 0,
				sign.getColor(), false, matrix.last().pose(), buffer, false, 0, combinedLights);
		});
	}

}
