package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.resources.ResourceLocation;

public class FlippableModel {

	public final BlockModelPart model;
	public final BlockModelPart flippedModel;

	public FlippableModel(BlockModelPart model, BlockModelPart flippedModel) {
		this.model = model;
		this.flippedModel = flippedModel;
	}

	public BlockModelPart get(boolean isFlipped) { return isFlipped ? flippedModel : model; }

	public static FlippableModel loadFrom(ResourceLocation modelLocation, ResourceLocation modelLocationFlipped, ResourceLocation texture) {
		return new FlippableModel(
			RenderingUtil.loadModel(modelLocation, texture, RenderingUtil.IdentityModelState),
			RenderingUtil.loadModel(modelLocationFlipped, texture, RenderingUtil.IdentityModelState)
		);
	}

	public static FlippableModel loadFrom(ResourceLocation modelLocation, ResourceLocation modelLocationFlipped, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
		return new FlippableModel(
			RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState),
			RenderingUtil.loadModel(modelLocationFlipped, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState)
		);
	}

	public static FlippableModel loadSymmetrical(ResourceLocation modelLocation, ResourceLocation texture) {
		BlockModelPart model = RenderingUtil.loadModel(modelLocation, texture, RenderingUtil.IdentityModelState);
		return new FlippableModel(model, model);
	}

	public static FlippableModel loadSymmetrical(ResourceLocation modelLocation, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
		BlockModelPart model = RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState);
		return new FlippableModel(model, model);
	}

}
