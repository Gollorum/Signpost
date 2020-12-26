package gollorum.signpost.utils.modelGeneration;

import net.minecraftforge.client.model.generators.ModelBuilder;

public enum FaceRotation {
    Zero(ModelBuilder.FaceRotation.ZERO),
    Clockwise90(ModelBuilder.FaceRotation.CLOCKWISE_90),
    CounterClockwise90(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90),
    UpsideDown(ModelBuilder.FaceRotation.UPSIDE_DOWN);

    public final ModelBuilder.FaceRotation asMinecraft;

    FaceRotation(ModelBuilder.FaceRotation asMinecraft) {
        this.asMinecraft = asMinecraft;
    }
}
