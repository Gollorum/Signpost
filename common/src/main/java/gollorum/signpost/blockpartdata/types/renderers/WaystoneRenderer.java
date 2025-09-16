package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.WaystoneBlockPart;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.minecraft.models.WaystoneInPostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.minecraft.rendering.TexturedModel;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WaystoneRenderer extends BlockPartRenderer<WaystoneBlockPart> {


	@Override
	public void render(
		WaystoneBlockPart part,
		BlockEntity tileEntity,
		PoseStack blockToView,
		MultiBufferSource buffer,
		int combinedLights,
		int combinedOverlay
	) {
		RenderingUtil.render(
			blockToView,
			new TexturedModel(
				WaystoneInPostModel.MODEL,
				TextureResource.waystoneTextureLocation.toMaterial(),
				Colors.white
			),
			buffer,
			combinedLights,
			combinedOverlay,
			RenderType::entitySolid
		);
	}

	@Override
	public void renderGui(WaystoneBlockPart part, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, MultiBufferSource buffer) {
		RenderingUtil.renderGui(
			new TexturedModel(
				WaystoneInPostModel.MODEL,
				TextureResource.waystoneTextureLocation.toMaterial(),
				Colors.white
			),
			matrixStack,
			center, yaw, pitch, isFlipped, scale, offset,
			buffer, m -> {});
	}

	@Override
	public void renderGui(WaystoneBlockPart waystone, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		RenderingUtil.renderGui(
			new TexturedModel(
				WaystoneInPostModel.MODEL,
				TextureResource.waystoneTextureLocation.toMaterial(),
				Colors.white
			),
			matrixStack, offset, Angle.ZERO,
			buffer,
			combinedLight, combinedOverlay, m -> {});
	}

}
