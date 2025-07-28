package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public record TexturedModel(ModelPart model, ResourceLocation texture, int tint) {}