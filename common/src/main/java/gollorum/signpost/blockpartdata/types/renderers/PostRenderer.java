package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.models.PostModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.minecraft.rendering.TexturedModel;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PostRenderer extends BlockPartRenderer<PostBlockPart> {

	@Override
	public void render(
		PostBlockPart post,
		BlockEntity tileEntity,
		PoseStack blockToView,
		MultiBufferSource buffer,
		int combinedLights,
		int combinedOverlay
	) {
		RenderingUtil.render(
            blockToView,
			new TexturedModel(
				PostModel.MODEL,
				post.getTexture().toMaterial(),
				post.getTexture().tint().map(tint -> tint.getColorAt(tileEntity.getLevel(), tileEntity.getBlockPos())).orElse(Colors.white)
			),
			buffer,
			combinedLights,
            combinedOverlay,
			RenderType::entityCutout
        );
	}

	@Override
	public void renderGui(PostBlockPart post, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset, MultiBufferSource buffer) {
		RenderingUtil.renderGui(
			new TexturedModel(
				PostModel.MODEL,
				post.getTexture().toMaterial(),
				post.getTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white)
			),
			matrixStack,
			center, yaw, pitch, isFlipped, scale, offset,
			buffer,
			m -> {}
		);
	}

	@Override
	public void renderGui(PostBlockPart post, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		RenderingUtil.renderGui(
			new TexturedModel(
				PostModel.MODEL,
				post.getTexture().toMaterial(),
				post.getTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white)
			),
			matrixStack,
			offset,
			Angle.ZERO,
			buffer,
			combinedLight,
			combinedOverlay,
			m -> {}
		);
	}

}
