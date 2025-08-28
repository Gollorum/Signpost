package gollorum.signpost.data.modelGeneration;

import gollorum.signpost.minecraft.models.modelGeneration.Cube;
import gollorum.signpost.minecraft.models.modelGeneration.FaceData;
import gollorum.signpost.minecraft.models.modelGeneration.FaceRotation;
import gollorum.signpost.minecraft.models.modelGeneration.TextureArea;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ModelBuilder;

import java.util.Map;
import java.util.function.BiConsumer;

public class ModelBuilderFactory {

    public static final BiConsumer<BlockModelBuilder, Cube<String>> BlockModel = (b, cube) -> {
        BlockModelBuilder.ElementBuilder builder = b.element()
            .from(cube.from.x(), cube.from.y(), cube.from.z())
            .to(cube.to.x(), cube.to.y(), cube.to.z());
        for (Map.Entry<Direction, FaceData<String>> face : cube.sides.entrySet()) {
            Direction dir = face.getKey();
            FaceData<String> faceData = face.getValue();
            TextureArea textureArea = faceData.textureArea().rotate(faceData.rotation(), true);
            ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder faceBuilder = builder.face(dir)
                .texture(faceData.texture())
                .uvs(textureArea.u.from, textureArea.v.from, textureArea.u.to, textureArea.v.to)
                .tintindex(faceData.tintIndex());
            if (!faceData.rotation().equals(FaceRotation.Zero))
                faceBuilder.rotation(FaceRotationUtils.asMinecraft(faceData.rotation()));
        }
    };

    public static final BiConsumer<BlockModelBuilder, Cube<String>> BlockModelFlipped = (b, cube) -> {
        BlockModelBuilder.ElementBuilder builder = b.element()
            .from(cube.from.x(), cube.from.y(), -cube.to.z())
            .to(cube.to.x(), cube.to.y(), -cube.from.z());
        for (Map.Entry<Direction, FaceData<String>> face : cube.sides.entrySet()) {
            Direction dir = face.getKey();
            Direction.Axis axis = dir.getAxis();
            FaceData<String> faceData = face.getValue();
            TextureArea textureArea = faceData.textureArea();
            if (axis.equals(Direction.Axis.Z)) {
                dir = dir.getOpposite();
                textureArea = textureArea.flipU();
            } else if (axis.equals(Direction.Axis.X)) {
                textureArea = textureArea.flipU();
            } else {
                textureArea = textureArea.flipV();
            }
            textureArea = textureArea.rotate(faceData.rotation(), true);
            ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder faceBuilder = builder.face(dir)
                .texture(faceData.texture())
                .uvs(textureArea.u.from, textureArea.v.from, textureArea.u.to, textureArea.v.to)
                .tintindex(faceData.tintIndex());
            if (!faceData.rotation().equals(FaceRotation.Zero))
                faceBuilder.rotation(FaceRotationUtils.asMinecraft(faceData.rotation()));
        }
    };
}
