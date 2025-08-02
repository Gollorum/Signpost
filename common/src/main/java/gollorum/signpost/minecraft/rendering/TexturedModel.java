package gollorum.signpost.minecraft.rendering;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.Material;

public record TexturedModel(ModelPart model, Material texture, int tint) {
    public TexturedModel(PartDefinition model, Material material, int tint) {
        this(model, material, tint, material.sprite().contents());
    }
    private TexturedModel(PartDefinition model, Material material, int tint, SpriteContents c) {
        this(model.bake(c.width(), c.height()), material, tint);
    }
}