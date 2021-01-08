package gollorum.signpost.signdata.types.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.signdata.Overlay;
import gollorum.signpost.signdata.types.SmallShortSign;
import gollorum.signpost.utils.math.MathUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class ShortSignRenderer extends SignRenderer<SmallShortSign> {

	private static final float TEXT_OFFSET_RIGHT = -3f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT = 13.5f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	private static final Map<ResourceLocation, Map<ResourceLocation, IBakedModel>> cachedModels = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, Map<ResourceLocation, IBakedModel>> cachedFlippedModels = new ConcurrentHashMap<>();

	private static final Map<ResourceLocation, IBakedModel> cachedOverlayModels = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, IBakedModel> cachedFlippedOverlayModels = new ConcurrentHashMap<>();

	@Override
	protected IBakedModel makeModel(SmallShortSign sign) {
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
	protected IBakedModel makeOverlayModel(SmallShortSign sign, Overlay overlay) {
		ResourceLocation texture = overlay.textureFor(SmallShortSign.class);
		return (sign.isFlipped() ? cachedFlippedOverlayModels : cachedOverlayModels)
			.computeIfAbsent(texture,
				x -> RenderingUtil.loadModel(
					sign.isFlipped() ? PostModel.largeOverlayFlippedLocation : PostModel.largeOverlayLocation,
					texture
				));
	}

	@Override
	protected void renderText(SmallShortSign sign, MatrixStack matrix, FontRenderer fontRenderer, IRenderTypeBuffer buffer, int combinedLights) {
		matrix.rotate(Vector3f.ZP.rotationDegrees(180));
		float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
		float MAX_WIDTH_FRAC = fontRenderer.getStringWidth(sign.getText()) * scale / MAXIMUM_TEXT_WIDTH;
		scale /= Math.max(1, MAX_WIDTH_FRAC);
		matrix.rotate(Vector3f.YP.rotation((float) (
			sign.isFlipped()
				? -sign.getAngle().radians()
				: Math.PI - sign.getAngle().radians())));
		float offset = MathUtils.lerp(TEXT_OFFSET_RIGHT, (TEXT_OFFSET_RIGHT - TEXT_OFFSET_LEFT) / 2f, 1 - Math.min(1, MAX_WIDTH_FRAC));
		matrix.translate(
			sign.isFlipped() ? offset - fontRenderer.getStringWidth(sign.getText()) * scale : -offset,
			-scale * 4 * TEXT_RATIO,
			-0.505 * VoxelSize);
		matrix.scale(scale, scale * TEXT_RATIO, scale);
		fontRenderer.renderString(sign.getText(), 0, 0, sign.getColor(), false, matrix.getLast().getMatrix(), buffer, false, 0, combinedLights);
	}

}
