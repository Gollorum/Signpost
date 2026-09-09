package gollorum.signpost.minecraft.rendering;

import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import net.minecraft.client.resources.model.sprite.SpriteId;

public record TexturedModel(QuadModel model, SpriteId texture, int tint) { }