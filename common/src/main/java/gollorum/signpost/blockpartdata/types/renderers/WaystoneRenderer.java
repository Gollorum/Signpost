package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.WaystoneBlockPart;
import gollorum.signpost.minecraft.gui.WaystoneModelResources;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.Lazy;
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
			localToBlock.last().pose(),
			RenderingUtil.loadModel(WaystoneModelResources.inPostLocation, RenderingUtil.IdentityModelState),
			tileEntity.getLevel(),
			tileEntity.getBlockState(),
			tileEntity.getBlockPos(),
			buffer.getBuffer(RenderType.solid()),
			false,
			random,
			randomSeed,
			combinedOverlay,
			new int[0]
		);
	}

	@Override
	public void renderGui(WaystoneBlockPart part, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset) {
		RenderingUtil.renderGui(RenderingUtil.loadModel(WaystoneModelResources.inPostLocation, RenderingUtil.IdentityModelState), matrixStack, new int[0], center, yaw, pitch, isFlipped, scale, offset, RenderType.solid(), m -> {});
	}

	@Override
	public void renderGui(WaystoneBlockPart waystone, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		RenderingUtil.renderGui(RenderingUtil.loadModel(WaystoneModelResources.inPostLocation, RenderingUtil.IdentityModelState), matrixStack, new int[0], offset, Angle.ZERO, buffer.getBuffer(RenderType.solid()), RenderType.solid(), combinedLight, combinedOverlay, m -> {});
	}

}
