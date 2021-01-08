package gollorum.signpost.signdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.signdata.Overlay;
import gollorum.signpost.signdata.types.LargeSign;
import gollorum.signpost.signdata.types.SmallWideSign;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class LargeSignRenderer extends SignRenderer<LargeSign> {

	private static final float TEXT_OFFSET_RIGHT = 7f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT_SHORT = 9f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT_LONG = 10f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH_SHORT = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT_SHORT;
	private static final float MAXIMUM_TEXT_WIDTH_LONG = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT_LONG;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	private static final Map<ResourceLocation, Map<ResourceLocation, IBakedModel>> cachedModels = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, Map<ResourceLocation, IBakedModel>> cachedFlippedModels = new ConcurrentHashMap<>();

	private static final Map<ResourceLocation, IBakedModel> cachedOverlayModels = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, IBakedModel> cachedFlippedOverlayModels = new ConcurrentHashMap<>();

	@Override
	protected IBakedModel makeModel(LargeSign sign) {
		return (sign.isFlipped() ? cachedFlippedModels : cachedModels)
			.computeIfAbsent(sign.getMainTexture(), x -> new ConcurrentHashMap<>())
			.computeIfAbsent(sign.getSecondaryTexture(),
				x -> RenderingUtil.loadModel(
					sign.isFlipped() ? PostModel.largeFlippedLocation : PostModel.largeLocation,
					sign.getMainTexture(), sign.getSecondaryTexture()
				)
			);
	}

	@Override
	protected IBakedModel makeOverlayModel(LargeSign sign, Overlay overlay) {
		ResourceLocation texture = overlay.textureFor(LargeSign.class);
		return (sign.isFlipped() ? cachedFlippedOverlayModels : cachedOverlayModels)
			.computeIfAbsent(texture,
				x -> RenderingUtil.loadModel(
					sign.isFlipped() ? PostModel.largeOverlayFlippedLocation : PostModel.largeOverlayLocation,
					texture
				));
	}

	@Override
	public void renderText(LargeSign sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights) {
		matrix.rotate(Vector3f.ZP.rotationDegrees(180));
		matrix.rotate(Vector3f.YP.rotation((float) (
			sign.isFlipped()
				? sign.getAngle().radians()
				: Math.PI - sign.getAngle().radians())));
		matrix.translate(0, 3.5f * VoxelSize, -3.005 * VoxelSize);

		matrix.push();
		render(sign, fontRenderer, sign.getText()[3], matrix, buffer, combinedLights, false);
		matrix.pop();
		matrix.translate(0, -7 / 3f * VoxelSize, 0);

		matrix.push();
		render(sign, fontRenderer, sign.getText()[2], matrix, buffer, combinedLights, true);
		matrix.pop();
		matrix.translate(0, -7 / 3f * VoxelSize, 0);

		matrix.push();
		render(sign, fontRenderer, sign.getText()[1], matrix, buffer, combinedLights, true);
		matrix.pop();
		matrix.translate(0, -7 / 3f * VoxelSize, 0);

		matrix.push();
		render(sign, fontRenderer, sign.getText()[0], matrix, buffer, combinedLights, false);
		matrix.pop();
	}

	private void render(LargeSign sign, FontRenderer fontRenderer, String text, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLights, boolean isLong) {
		float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
		float MAX_WIDTH_FRAC = fontRenderer.getStringWidth(text) * scale / (isLong ? MAXIMUM_TEXT_WIDTH_LONG : MAXIMUM_TEXT_WIDTH_SHORT);
		scale /= Math.max(1, MAX_WIDTH_FRAC);
		float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
		matrix.translate(
			sign.isFlipped() ? offset - fontRenderer.getStringWidth(text) * scale : -offset,
			-scale * 4 * TEXT_RATIO,
			0
		);
		matrix.scale(scale, scale * TEXT_RATIO, scale);
		fontRenderer.renderString(text, 0, 0,
			sign.getColor(), false, matrix.getLast().getMatrix(), buffer, false, 0, combinedLights);
	}

}
