package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

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

//	public static FlippableModel loadFrom(Identifier modelLocation, Identifier modelLocationFlipped, Identifier texture) {
//		return new FlippableModel(
//			RenderingUtil.loadModel(modelLocation, texture, RenderingUtil.IdentityModelState),
//			RenderingUtil.loadModel(modelLocationFlipped, texture, RenderingUtil.IdentityModelState)
//		);
//	}
//
//	public static FlippableModel loadFrom(Identifier modelLocation, Identifier modelLocationFlipped, Identifier mainTexture, Identifier secondaryTexture) {
//		return new FlippableModel(
//			RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState),
//			RenderingUtil.loadModel(modelLocationFlipped, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState)
//		);
//	}
//
//	public static FlippableModel loadSymmetrical(Identifier modelLocation, Identifier texture) {
//		TintedModel model = RenderingUtil.loadModel(modelLocation, texture, RenderingUtil.IdentityModelState);
//		return new FlippableModel(model, model);
//	}
//
//	public static FlippableModel loadSymmetrical(Identifier modelLocation, Identifier mainTexture, Identifier secondaryTexture) {
//		TintedModel model = RenderingUtil.loadModel(modelLocation, mainTexture, secondaryTexture, RenderingUtil.IdentityModelState);
//		return new FlippableModel(model, model);
//	}

}
