package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.blockpartdata.types.BlockPartRenderer;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.data.PostModel;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.modelGeneration.SignModel;
import gollorum.signpost.utils.modelGeneration.SignModelFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PostRenderer extends BlockPartRenderer<PostBlockPart> {

	private final boolean shouldRenderBaked = true;

	private static final Map<ResourceLocation, SignModel> cachedModels = new ConcurrentHashMap<>();
	private static final Map<ResourceLocation, BakedModel> cachedBakedModels = new ConcurrentHashMap<>();

	private SignModel makeModel(PostBlockPart post) {
		return cachedModels
			.computeIfAbsent(post.getTexture().location(),
				x -> new SignModelFactory<ResourceLocation>()
					.makePost(x)
					.build(new SignModel(), SignModel::addCube)
			);
	}

	private BakedModel makeBakedModel(PostBlockPart post) {
		return cachedBakedModels
			.computeIfAbsent(post.getTexture().location(),
				x -> RenderingUtil.loadModel(
					PostModel.postLocation,
					post.getTexture().location()
				)
			);
	}

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
		if(shouldRenderBaked)
			RenderingUtil.render(
				blockToView,
				localToBlock.last().pose(),
				makeBakedModel(post),
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
		else makeModel(post).render(
			blockToView.last().pose(),
			localToBlock.last().pose(),
			buffer,
			RenderType.solid(),
			combinedLights,
			combinedOverlay,
			true,
			tileEntity.getLevel(),
			tileEntity.getBlockState(),
			tileEntity.getBlockPos(),
			tints
		);
	}

	@Override
	public void renderGui(PostBlockPart post, PoseStack matrixStack, Point center, Angle yaw, Angle pitch, boolean isFlipped, float scale, Vector3 offset) {
		var tints = new int[]{post.getTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white)};
		RenderingUtil.renderGui(makeBakedModel(post), matrixStack, tints, center, yaw, pitch, isFlipped, scale, offset, RenderType.solid(), m -> {});
	}

	@Override
	public void renderGui(PostBlockPart post, PoseStack matrixStack, Vector3 offset, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		var tints = new int[]{post.getTexture().tint().map(t -> t.getColorAt(Minecraft.getInstance().level, Minecraft.getInstance().player.blockPosition())).orElse(Colors.white)};
		RenderingUtil.renderGui(makeBakedModel(post), matrixStack, tints, offset, Angle.ZERO, buffer.getBuffer(RenderType.solid()), RenderType.solid(), combinedLight, combinedOverlay, m -> {});
	}

}
