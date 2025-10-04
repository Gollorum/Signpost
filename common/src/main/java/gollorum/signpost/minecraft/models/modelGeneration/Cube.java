package gollorum.signpost.minecraft.models.modelGeneration;

import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Cube<TextureIdentifier> {
    public final Vector3 from;
    public final Vector3 to;
    public final Map<Direction, FaceData<TextureIdentifier>> sides;

    public Cube(Vector3 from, Vector3 to, Map<Direction, FaceData<TextureIdentifier>> sides) {
        this.from = from;
        this.to = to;
        this.sides = sides;
    }

	public Cube<TextureIdentifier> withSides(Function<FaceData<TextureIdentifier>, FaceData<TextureIdentifier>> func) {
    	return new Cube<>(from, to, sides.entrySet().stream().collect(
    		Collectors.toMap(Map.Entry::getKey, kv -> func.apply(kv.getValue()))));
	}

}
