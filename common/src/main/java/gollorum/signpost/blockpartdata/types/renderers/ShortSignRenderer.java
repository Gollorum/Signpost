package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.minecraft.models.ShortSignModel;
import gollorum.signpost.minecraft.rendering.ModelRegistry;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.MathUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class ShortSignRenderer extends SignRenderer<SmallShortSignBlockPart> {

	private static final float TEXT_OFFSET_RIGHT = -3f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT = 13.5f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	@Override
	protected PartDefinition makeMainModel(SmallShortSignBlockPart sign) {
		return (sign.isFlipped() ? ShortSignModel.MODEL_MAIN_FLIPPED : ShortSignModel.MODEL_MAIN);
	}

	@Override
	protected PartDefinition makeSecondaryModel(SmallShortSignBlockPart sign) {
		return (sign.isFlipped() ? ShortSignModel.MODEL_SECONDARY_FLIPPED : ShortSignModel.MODEL_SECONDARY);
	}

	@Override
	protected PartDefinition makeBakedOverlayModel(SmallShortSignBlockPart sign, Overlay overlay) {
		return (sign.isFlipped() ? ShortSignModel.MODEL_OVERLAY_FLIPPED : ShortSignModel.MODEL_OVERLAY);
	}

	@Override
	protected void renderText(SmallShortSignBlockPart sign, PoseStack matrix, Font fontRenderer, MultiBufferSource buffer, int combinedLights) {
		renderText(true, sign, matrix, fontRenderer, buffer, combinedLights);
		renderText(false, sign, matrix, fontRenderer, buffer, combinedLights);
	}

	private void renderText(boolean isFlipped, SmallShortSignBlockPart sign, PoseStack matrix, Font fontRenderer, MultiBufferSource buffer, int combinedLights) {
		RenderingUtil.wrapInMatrixEntry(matrix, () -> {
			var text = sign.getText().get();
			if(sign.isMarkedForGeneration()) {
				var overrideName = WaystoneLibrary.getInstance().getAllWaystoneNames(true)
						.flatMap(s -> s.stream().skip(new Random().nextInt(s.size())).findFirst());
				if(overrideName.isPresent()) text = overrideName.get();
			}
			matrix.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 0, 1))));
			float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
			float MAX_WIDTH_FRAC = fontRenderer.width(text) * scale / MAXIMUM_TEXT_WIDTH;
			scale /= Math.max(1, MAX_WIDTH_FRAC);
			boolean flipped = isFlipped ^ sign.isFlipped();
			if(isFlipped) matrix.mulPose(new Quaternionf(new AxisAngle4d(Math.PI, new Vector3f(0, 1, 0))));
			float offset = MathUtils.lerp(TEXT_OFFSET_RIGHT, (TEXT_OFFSET_RIGHT - TEXT_OFFSET_LEFT) / 2f, 1 - Math.min(1, MAX_WIDTH_FRAC));
			matrix.translate(
				flipped ? offset - fontRenderer.width(text) * scale : -offset,
				-scale * 4 * TEXT_RATIO,
				-0.505 * VoxelSize);
			matrix.scale(scale, scale * TEXT_RATIO, scale);
			fontRenderer.drawInBatch(text, 0, 0, sign.getColor(), false, matrix.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, combinedLights);
		});
	}

}
