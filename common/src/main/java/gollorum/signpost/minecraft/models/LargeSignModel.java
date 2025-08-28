package gollorum.signpost.minecraft.models;

import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import gollorum.signpost.minecraft.models.modelGeneration.SignModelFactory;

public class LargeSignModel {
    public static final QuadModel MODEL_MAIN;
    public static final QuadModel MODEL_MAIN_FLIPPED;
    public static final QuadModel MODEL_SECONDARY;
    public static final QuadModel MODEL_SECONDARY_FLIPPED;
    public static final QuadModel MODEL_OVERLAY;
    public static final QuadModel MODEL_OVERLAY_FLIPPED;

    static {
        var modelBuilder = new SignModelFactory<Integer>().makeLargeSign(0, 1);
        var overlayBuilder = new SignModelFactory<Integer>().makeLargeSignOverlay(0);

        var model = modelBuilder.build(QuadModel.builderForTwoTextures(), SignModelFactory.Builder.CUBE_LIST_BUILDER);
        var modelFlipped = modelBuilder.build(QuadModel.builderForTwoTextures(), SignModelFactory.Builder.CUBE_LIST_BUILDER_FLIPPED);
        var overlay = overlayBuilder.build(QuadModel.builderForSingleTexture(), SignModelFactory.Builder.CUBE_LIST_BUILDER);
        var overlayFlipped = overlayBuilder.build(QuadModel.builderForSingleTexture(), SignModelFactory.Builder.CUBE_LIST_BUILDER_FLIPPED);

        MODEL_MAIN = model[0];
        MODEL_MAIN_FLIPPED = modelFlipped[0];
        MODEL_SECONDARY = model[1];
        MODEL_SECONDARY_FLIPPED = modelFlipped[1];
        MODEL_OVERLAY = overlay[0];
        MODEL_OVERLAY_FLIPPED = overlayFlipped[0];
    }
}
