package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.WaystoneBlockPart;
import gollorum.signpost.minecraft.gui.WaystoneModelResources;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.minecraft.models.WaystoneInPostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WaystoneRenderer extends BlockPartRenderer<WaystoneBlockPart> {


	@Override
	public void render(
		WaystoneBlockPart part,
		BlockEntity tileEntity,
		BlockEntityRenderDispatcher renderDispatcher,
		PoseStack blockToView,
		PoseStack localToBlock,
		MultiBufferSource buffer,
		int combinedLights,
		int combinedOverlay,
		RandomSource random,
		long randomSeed
	) {
		RenderingUtil.render(
			blockToView,
			WaystoneInPostModel.MODEL.bake(16, 16), // TODO DS: ???
			buffer.getBuffer(RenderType.entitySolid(TextureResource.waystoneTextureLocation)),
			RenderingUtil.FLAT_LIGHT_PROBABLY,
			combinedOverlay,
			Colors.white
		);
	}

	@Override
	public void renderGui(WaystoneBlockPart part, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, MultiBufferSource buffer) {
		RenderingUtil.renderGui(
			WaystoneInPostModel.MODEL.bake(16, 16), // TODO DS: ???
			matrixStack,
			Colors.white,
			center, yaw, pitch, isFlipped, scale, offset,
			buffer.getBuffer(RenderType.guiTextured(TextureResource.waystoneTextureLocation)), m -> {});
	}

	@Override
	public void renderGui(WaystoneBlockPart waystone, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		RenderingUtil.renderGui(
			WaystoneInPostModel.MODEL.bake(16, 16), // TODO DS: ???
			matrixStack, offset, Angle.ZERO,
			buffer.getBuffer(RenderType.guiTextured(TextureResource.waystoneTextureLocation)),
			combinedLight, combinedOverlay, Colors.white, m -> {});
	}

}
