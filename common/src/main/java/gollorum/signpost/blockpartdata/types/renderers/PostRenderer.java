package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.gui.PostModelResources;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PostRenderer extends BlockPartRenderer<PostBlockPart> {

	@Override
	public void render(
		PostBlockPart post,
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
		var tints = new int[] {post.getTexture().tint().map(tint -> tint.getColorAt(tileEntity.getLevel(), tileEntity.getBlockPos())).orElse(Colors.white)};
		RenderingUtil.render(
            blockToView,
            localToBlock.last().pose(),
			RenderingUtil.loadModel(
				PostModelResources.postLocation,
				post.getTexture().location(),
				RenderingUtil.IdentityModelState
			),
            tileEntity.getLevel(),
            tileEntity.getBlockState(),
            tileEntity.getBlockPos(),
            buffer.getBuffer(RenderType.solid()),
            false,
            random,
            randomSeed,
            combinedOverlay,
            tints
        );
	}

	@Override
	public void renderGui(PostBlockPart post, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset) {
		var tints = new int[]{post.getTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white)};
		RenderingUtil.renderGui(RenderingUtil.loadModel(
			PostModelResources.postLocation,
			post.getTexture().location(),
			RenderingUtil.IdentityModelState
		), matrixStack, tints, center, yaw, pitch, isFlipped, scale, offset, RenderType.solid(), m -> {});
	}

	@Override
	public void renderGui(PostBlockPart post, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		var tints = new int[]{post.getTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white)};
		RenderingUtil.renderGui(RenderingUtil.loadModel(
			PostModelResources.postLocation,
			post.getTexture().location(),
			RenderingUtil.IdentityModelState
		), matrixStack, tints, offset, Angle.ZERO, buffer.getBuffer(RenderType.solid()), RenderType.solid(), combinedLight, combinedOverlay, m -> {});
	}

}
