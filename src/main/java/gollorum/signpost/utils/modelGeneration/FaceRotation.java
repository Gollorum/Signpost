package gollorum.signpost.utils.modelGeneration;

import net.minecraftforge.client.model.generators.ModelBuilder;

public enum FaceRotation {
    Zero(ModelBuilder.FaceRotation.ZERO),
    Clockwise90(ModelBuilder.FaceRotation.CLOCKWISE_90),
    CounterClockwise90(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90),
    UpsideDown(ModelBuilder.FaceRotation.UPSIDE_DOWN);

    public final ModelBuilder.FaceRotation asMinecraft;

    public FaceRotation inverse() {
        return switch (this) {
            case Zero -> Zero;
            case Clockwise90 -> CounterClockwise90;
            case CounterClockwise90 -> Clockwise90;
            case UpsideDown -> UpsideDown;
        };
    }

    public FaceRotation rotate180() {
        return switch (this) {
            case Zero -> UpsideDown;
            case Clockwise90 -> CounterClockwise90;
            case CounterClockwise90 -> Clockwise90;
            case UpsideDown -> Zero;
        };
    }

    FaceRotation(ModelBuilder.FaceRotation asMinecraft) {
        this.asMinecraft = asMinecraft;
    }
}
