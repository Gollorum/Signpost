package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

public class FlippableModel {

	public final TexturedModel[] model;
	public final TexturedModel[] flippedModel;

	public FlippableModel(TexturedModel[] model, TexturedModel[] flippedModel) {
		this.model = model;
		this.flippedModel = flippedModel;
	}

	public TexturedModel[] get(boolean isFlipped) { return isFlipped ? flippedModel : model; }

	public static FlippableModel from(TexturedModel[] models, TexturedModel[] flippedModels) {
		return new FlippableModel(models, flippedModels);
	}

	public static FlippableModel fromSymmetric(TexturedModel[] models) {
		return new FlippableModel(models, models);
	}

//	public static FlippableModel loadFrom(ResourceLocation modelLocation, ResourceLocation modelLocationFlipped, ResourceLocation texture) {
//		return new FlippableModel(
//			RenderingUtil.loadModel(modelLocation, texture, RenderingUtil.IdentityModelState),
//			RenderingUtil.loadModel(modelLocationFlipped, texture, RenderingUtil.IdentityModelState)
//		);
//	}
//
//	public static FlippableModel loadFrom(ResourceLocation modelLocation, ResourceLocation modelLocationFlipped, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
//		return new FlippableModel(
//			RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState),
//			RenderingUtil.loadModel(modelLocationFlipped, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState)
//		);
//	}
//
//	public static FlippableModel loadSymmetrical(ResourceLocation modelLocation, ResourceLocation texture) {
//		TintedModel model = RenderingUtil.loadModel(modelLocation, texture, RenderingUtil.IdentityModelState);
//		return new FlippableModel(model, model);
//	}
//
//	public static FlippableModel loadSymmetrical(ResourceLocation modelLocation, ResourceLocation mainTexture, ResourceLocation secondaryTexture) {
//		TintedModel model = RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState);
//		return new FlippableModel(model, model);
//	}

}
