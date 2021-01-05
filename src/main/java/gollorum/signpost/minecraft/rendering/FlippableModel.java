package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.ResourceLocation;

public class FlippableModel {

	public final IBakedModel model;
	public final IBakedModel flippedModel;

	public FlippableModel(IBakedModel model, IBakedModel flippedModel) {
		this.model = model;
		this.flippedModel = flippedModel;
	}

	public IBakedModel get(boolean isFlipped) { return isFlipped ? flippedModel : model; }

	public FlippableModel withTintIndex(int tintIndex) {
		return new FlippableModel(
			RenderingUtil.withTintIndex(model, tintIndex),
			RenderingUtil.withTintIndex(flippedModel, tintIndex)
		);
	}

	public static FlippableModel loadFrom(ResourceLocation modelLocation, ResourceLocation modelLocationFlipped, ResourceLocation texture) {
		return new FlippableModel(
			RenderingUtil.loadModel(modelLocation, texture).get(),
			RenderingUtil.loadModel(modelLocationFlipped, texture).get()
		);
	}

	public static FlippableModel loadFrom(ResourceLocation modelLocation, ResourceLocation modelLocationFlipped, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
		return new FlippableModel(
			RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture).get(),
			RenderingUtil.loadModel(modelLocationFlipped, mainTexture, secondaryTexture).get()
		);
	}

	public static FlippableModel loadSymmetrical(ResourceLocation modelLocation, ResourceLocation texture) {
		IBakedModel model = RenderingUtil.loadModel(modelLocation, texture).get();
		return new FlippableModel(model, model);
	}

	public static FlippableModel loadSymmetrical(ResourceLocation modelLocation, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
		IBakedModel model = RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture).get();
		return new FlippableModel(model, model);
	}

}
