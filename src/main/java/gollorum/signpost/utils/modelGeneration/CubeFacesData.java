package gollorum.signpost.utils.modelGeneration;

import gollorum.signpost.utils.Tuple;
import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record CubeFacesData<TextureIdentifier>(Direction direction, TextureIdentifier texture, FaceRotation rotation, int textureSize, int tintIndex) {

    public static <TextureIdentifier> List<CubeFacesData<TextureIdentifier>> from(
        Function<
            Direction,
            Optional<Tuple<Tuple<TextureIdentifier, FaceRotation>, Tuple<Integer, Integer>>>
        > faceDataGetter) {
        return Arrays.stream(Direction.values())
            .map(d -> faceDataGetter.apply(d).map(fd -> Tuple.of(d, fd)))
            .filter(Optional::isPresent).map(Optional::get)
            .map(Tuple -> new CubeFacesData<>(Tuple._1, Tuple._2._1._1, Tuple._2._1._2, Tuple._2._2._1, Tuple._2._2._2))
            .collect(Collectors.toList());
    }

    public static <TextureIdentifier> List<CubeFacesData<TextureIdentifier>> uniform(TextureIdentifier texture, FaceRotation rotation, int textureSize, int tintIndex, Direction... directions) {
        return Arrays.stream(directions).map(d -> new CubeFacesData<>(d, texture, rotation, textureSize, tintIndex)).collect(Collectors.toList());
    }

    public static <TextureIdentifier> List<CubeFacesData<TextureIdentifier>> all(TextureIdentifier texture, FaceRotation rotation, int textureSize, int tintIndex, Predicate<Direction> where) {
        return Arrays.stream(Direction.values())
            .filter(where)
            .map(d -> new CubeFacesData<>(d, texture, rotation, textureSize, tintIndex))
            .collect(Collectors.toList());
    }

}
