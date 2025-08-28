package gollorum.signpost.minecraft.rendering;

import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import net.minecraft.client.resources.model.Material;

public record TexturedModel(QuadModel model, Material texture, int tint) { }