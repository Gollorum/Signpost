package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.gui.PostModelResources;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.models.PostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
			PostModel.MODEL.bake(16, 16), // TODO DS: ???
			buffer.getBuffer(
				RenderType.entityCutout(post.getTexture().location())
			),
			combinedLights,
            combinedOverlay,
            tints[0]
        );
	}

	@Override
	public void renderGui(PostBlockPart post, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, MultiBufferSource buffer) {
		var color = post.getTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white);
		RenderingUtil.render(
			matrixStack,
			PostModel.MODEL.bake(16, 16), // TODO DS: ???
			buffer.getBuffer(RenderType.guiTextured(post.getTexture().location())),
			RenderingUtil.FLAT_LIGHT_PROBABLY,
			OverlayTexture.NO_OVERLAY,
			color
		);
	}

	@Override
	public void renderGui(PostBlockPart post, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		var color = post.getTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white);
		RenderingUtil.render(
			matrixStack,
			PostModel.MODEL.bake(16, 16), // TODO DS: ???
			buffer.getBuffer(RenderType.guiTextured(post.getTexture().location())),
			combinedLight,
			combinedOverlay,
			color
		);
	}

}
