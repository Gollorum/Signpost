package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class FlippableModel {

	public final BakedModel model;
	public final BakedModel flippedModel;

	public FlippableModel(BakedModel model, BakedModel flippedModel) {
		this.model = model;
		this.flippedModel = flippedModel;
	}

	public BakedModel get(boolean isFlipped) { return isFlipped ? flippedModel : model; }

	public FlippableModel withTintIndex(int tintIndex) {
		return new FlippableModel(
			RenderingUtil.withTintIndex(model, tintIndex),
			RenderingUtil.withTintIndex(flippedModel, tintIndex)
		);
	}

	public static FlippableModel loadFrom(ResourceLocation modelLocation, ResourceLocation modelLocationFlipped, ResourceLocation texture) {
		return new FlippableModel(
			RenderingUtil.loadModel(modelLocation, texture),
			RenderingUtil.loadModel(modelLocationFlipped, texture)
		);
	}

	public static FlippableModel loadFrom(ResourceLocation modelLocation, ResourceLocation modelLocationFlipped, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
		return new FlippableModel(
			RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture),
			RenderingUtil.loadModel(modelLocationFlipped, mainTexture, secondaryTexture)
		);
	}

	public static FlippableModel loadSymmetrical(ResourceLocation modelLocation, ResourceLocation texture) {
		BakedModel model = RenderingUtil.loadModel(modelLocation, texture);
		return new FlippableModel(model, model);
	}

	public static FlippableModel loadSymmetrical(ResourceLocation modelLocation, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
		BakedModel model = RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture);
		return new FlippableModel(model, model);
	}

}
