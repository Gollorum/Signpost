package gollorum.signpost.data.modelGeneration;

import gollorum.signpost.minecraft.models.modelGeneration.FaceRotation;
import net.minecraftforge.client.model.generators.ModelBuilder;

public class FaceRotationUtils {

    public static ModelBuilder.FaceRotation asMinecraft(FaceRotation rot) {
        return switch (rot) {
            case Zero -> ModelBuilder.FaceRotation.ZERO;
            case Clockwise90 -> ModelBuilder.FaceRotation.CLOCKWISE_90;
            case CounterClockwise90 -> ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90;
            case UpsideDown -> ModelBuilder.FaceRotation.UPSIDE_DOWN;
        };
    }

}
