package gollorum.signpost.minecraft.models.modelGeneration;

import java.util.ArrayList;
import java.util.List;

public record QuadModel(List<Quad> quads) {

    public QuadModel() { this(new ArrayList<>()); }

    public static QuadModel[] builderForSingleTexture() {
        return new QuadModel[] { new QuadModel() };
    }

    public static QuadModel[] builderForTwoTextures() {
        return new QuadModel[] { new QuadModel(), new QuadModel() };
    }
}
