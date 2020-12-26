package gollorum.signpost.utils.modelGeneration;

import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Cube<TextureIdentifier> {
    public final Vector3 min;
    public final Vector3 max;
    public final Map<Direction, FaceData<TextureIdentifier>> sides;

    public Cube(Vector3 min, Vector3 max, Map<Direction, FaceData<TextureIdentifier>> sides) {
        this.min = min;
        this.max = max;
        this.sides = sides;
    }

	public Cube<TextureIdentifier> withSides(Function<FaceData<TextureIdentifier>, FaceData<TextureIdentifier>> func) {
    	return new Cube<>(min, max, sides.entrySet().stream().collect(
    		Collectors.toMap(Map.Entry::getKey, kv -> func.apply(kv.getValue()))));
	}

	public List<Quad<TextureIdentifier>> getQuads() {
		return sides.entrySet().stream().map(e -> getQuad(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	private Quad<TextureIdentifier> getQuad(Direction direction, FaceData<TextureIdentifier> faceData) {
		float fromU = faceData.textureArea.u.from;
		float toU = faceData.textureArea.u.to;
		float fromV = faceData.textureArea.v.from;
		float toV = faceData.textureArea.v.to;
		switch (direction) {
			case DOWN:
				return new Quad<>(
					new Vertex[]{
						new Vertex(min.withX(max.x), toU, toV),
						new Vertex(max.withY(min.y), toU, fromV),
						new Vertex(min.withZ(max.z), fromU, fromV),
						new Vertex(min, fromU, toV),
					},
					new Vector3(0, -1, 0),
					faceData);
			case UP:
				return new Quad<>(
					new Vertex[]{
						new Vertex(max.withZ(min.z), toU, fromV),
						new Vertex(min.withY(max.y), fromU, fromV),
						new Vertex(max.withX(min.x), fromU, toV),
						new Vertex(max, toU, toV),
					},
					new Vector3(0, 1, 0),
					faceData);
			case NORTH:
				return new Quad<>(
					new Vertex[]{
						new Vertex(max.withX(min.x), toU, toV),
						new Vertex(min.withZ(max.z), toU, fromV),
						new Vertex(max.withY(min.y), fromU, fromV),
						new Vertex(max, fromU, toV),
					},
					new Vector3(0, 0, 1),
					faceData);
			case SOUTH:
				return new Quad<>(
					new Vertex[]{
						new Vertex(min.withY(max.y), fromU, toV),
						new Vertex(max.withZ(min.z), toU, toV),
						new Vertex(min.withX(max.x), toU, fromV),
						new Vertex(min, fromU, fromV),
					},
					new Vector3(0, 0, 1),
					faceData);
			case WEST:
				return new Quad<>(
					new Vertex[]{
						new Vertex(min.withZ(max.z), fromU, fromV),
						new Vertex(max.withX(min.x), fromU, toV),
						new Vertex(min.withY(max.y), toU, toV),
						new Vertex(min, toU, fromV),
					},
					new Vector3(-1, 0, 0),
					faceData);
			case EAST:
				return new Quad<>(
					new Vertex[]{
						new Vertex(max.withY(min.y), toU, fromV),
						new Vertex(min.withX(max.x), fromU, fromV),
						new Vertex(max.withZ(min.z), fromU, toV),
						new Vertex(max, toU, toV),
					},
					new Vector3(1, 0, 0),
					faceData);
			default:
				throw new RuntimeException("Direction " + direction + " could not be handled");
		}
	}

	public static class Quad<TextureIdentifier> {
    	public final Vertex[] vertices;
    	public final Vector3 normal;
    	public final FaceData<TextureIdentifier> faceData;

		public Quad(Vertex[] vertices, Vector3 normal, FaceData<TextureIdentifier> faceData) {
			this.vertices = vertices;
			this.normal = normal;
			this.faceData = faceData;
		}

	}

	public static class Vertex {
		public final Vector3 pos;
		public final float u;
		public final float v;

		public Vertex(Vector3 pos, float u, float v) {
			this.pos = pos;
			this.u = u;
			this.v = v;
		}
	}

}
