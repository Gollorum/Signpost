package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.SmallWideSign;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class WideSignRenderer extends SignRenderer<SmallWideSign> {

	private static final float TEXT_OFFSET_RIGHT = 7f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT = 11f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	private static final Map<ResourceLocation, Map<ResourceLocation, IBakedModel>> cachedModels = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, Map<ResourceLocation, IBakedModel>> cachedFlippedModels = new ConcurrentHashMap<>();

	private static final Map<ResourceLocation, IBakedModel> cachedOverlayModels = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, IBakedModel> cachedFlippedOverlayModels = new ConcurrentHashMap<>();

	@Override
	protected IBakedModel makeModel(SmallWideSign sign) {
		return (sign.isFlipped() ? cachedFlippedModels : cachedModels)
			.computeIfAbsent(sign.getMainTexture(), x -> new ConcurrentHashMap<>())
			.computeIfAbsent(sign.getSecondaryTexture(),
				x -> RenderingUtil.loadModel(
					sign.isFlipped() ? PostModel.wideFlippedLocation : PostModel.wideLocation,
					sign.getMainTexture(), sign.getSecondaryTexture()
				)
		);
	}

	@Override
	protected IBakedModel makeOverlayModel(SmallWideSign sign, Overlay overlay) {
		ResourceLocation texture = overlay.textureFor(SmallWideSign.class);
		return (sign.isFlipped() ? cachedFlippedOverlayModels : cachedOverlayModels)
			.computeIfAbsent(texture,
				x -> RenderingUtil.loadModel(
					sign.isFlipped() ? PostModel.wideOverlayFlippedLocation : PostModel.wideOverlayLocation,
					texture
		));
	}

	@Override
	protected void renderText(SmallWideSign sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights) {
		matrix.rotate(Vector3f.ZP.rotationDegrees(180));
		float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
		float MAX_WIDTH_FRAC = fontRenderer.getStringWidth(sign.getText()) * scale / MAXIMUM_TEXT_WIDTH;
		scale /= Math.max(1, MAX_WIDTH_FRAC);
		matrix.rotate(Vector3f.YP.rotation((float) (
			sign.isFlipped()
				? -sign.getAngle().radians()
				: Math.PI - sign.getAngle().radians())));
		float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
		matrix.translate(
			sign.isFlipped() ? offset - fontRenderer.getStringWidth(sign.getText()) * scale : -offset,
			-scale * 4 * TEXT_RATIO,
			-3.005 * VoxelSize);
		matrix.scale(scale, scale * TEXT_RATIO, scale);
		fontRenderer.renderString(sign.getText(), 0, 0, sign.getColor(), false, matrix.getLast().getMatrix(), buffer, false, 0, combinedLights);
	}

}
