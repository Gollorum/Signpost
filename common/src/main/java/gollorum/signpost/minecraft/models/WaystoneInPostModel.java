package gollorum.signpost.minecraft.models;

import gollorum.signpost.minecraft.models.modelGeneration.QuadModel;
import gollorum.signpost.minecraft.models.modelGeneration.SignModelFactory;

public class WaystoneInPostModel {

    public static final QuadModel MODEL;

    static {
        MODEL = new SignModelFactory<Integer>()
            .makeWaystoneInPost(0)
            .build(QuadModel.builderForSingleTexture(), SignModelFactory.Builder.CUBE_LIST_BUILDER)
            [0];
    }

}
