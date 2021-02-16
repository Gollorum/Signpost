package gollorum.signpost.utils.modelGeneration;

import net.minecraftforge.client.model.generators.ModelBuilder;

public enum FaceRotation {
    Zero(ModelBuilder.FaceRotation.ZERO),
    Clockwise90(ModelBuilder.FaceRotation.CLOCKWISE_90),
    CounterClockwise90(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90),
    UpsideDown(ModelBuilder.FaceRotation.UPSIDE_DOWN);

    public final ModelBuilder.FaceRotation asMinecraft;

    public static FaceRotation inverseOf(FaceRotation rot) {
        switch(rot) {
            case Zero: return Zero;
            case Clockwise90: return CounterClockwise90;
            case CounterClockwise90: return Clockwise90;
            case UpsideDown: return UpsideDown;
            default: throw new RuntimeException("Face rotation " + rot + " is not supported");
        }
    }

    FaceRotation(ModelBuilder.FaceRotation asMinecraft) {
        this.asMinecraft = asMinecraft;
    }
}
