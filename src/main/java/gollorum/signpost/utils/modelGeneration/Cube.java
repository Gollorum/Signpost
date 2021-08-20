package gollorum.signpost.utils.modelGeneration;

import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.util.Direction;

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

	public List<Quad<TextureIdentifier>> getQuads() {
		return sides.entrySet().stream().map(e -> getQuad(e.getKey(), e.getValue())).collect(Collectors.toList());
	}

	private Quad<TextureIdentifier> getQuad(Direction direction, FaceData<TextureIdentifier> faceData) {
		float fromU = faceData.textureArea.u.to;
		float toU = faceData.textureArea.u.from;
		float fromV = faceData.textureArea.v.from;
		float toV = faceData.textureArea.v.to;
		Quad<TextureIdentifier> quad;
		switch (direction) {
			case DOWN:
				quad = new Quad<>(
					new Vertex[]{
						new Vertex(from.withX(to.x), toU, toV),
						new Vertex(to.withY(from.y), toU, fromV),
						new Vertex(from.withZ(to.z), fromU, fromV),
						new Vertex(from, fromU, toV),
					},
					new Vector3(0, -1, 0),
					faceData);
				break;
			case UP:
				quad = new Quad<>(
					new Vertex[]{
						new Vertex(to.withZ(from.z), toU, fromV),
						new Vertex(from.withY(to.y), fromU, fromV),
						new Vertex(to.withX(from.x), fromU, toV),
						new Vertex(to, toU, toV),
					},
					new Vector3(0, 1, 0),
					faceData);
				break;
			case SOUTH:
				quad = new Quad<>(
					new Vertex[]{
						new Vertex(to.withY(from.y), fromU, toV),
						new Vertex(to, fromU, fromV),
						new Vertex(to.withX(from.x), toU, fromV),
						new Vertex(from.withZ(to.z), toU, toV),
					},
					new Vector3(0, 0, 1),
					faceData);
				break;
			case NORTH:
				quad = new Quad<>(
					new Vertex[]{
						new Vertex(from.withY(to.y), fromU, fromV),
						new Vertex(to.withZ(from.z), toU, fromV),
						new Vertex(from.withX(to.x), toU, toV),
						new Vertex(from, fromU, toV),
					},
					new Vector3(0, 0, -1),
					faceData);
				break;
			case EAST:
				quad = new Quad<>(
					new Vertex[]{
						new Vertex(from.withX(to.x), fromU, toV),
						new Vertex(to.withZ(from.z), fromU, fromV),
						new Vertex(to, toU, fromV),
						new Vertex(to.withY(from.y), toU, toV),
					},
					new Vector3(1, 0, 0),
					faceData);
				break;
			case WEST:
				quad = new Quad<>(
					new Vertex[]{
						new Vertex(from, toU, toV),
						new Vertex(from.withZ(to.z), fromU, toV),
						new Vertex(to.withX(from.x), fromU, fromV),
						new Vertex(from.withY(to.y), toU, fromV),
					},
					new Vector3(-1, 0, 0),
					faceData);
				break;
			default:
				throw new RuntimeException("Direction " + direction + " could not be handled");
		}
		if(faceData.shouldFlipNormal) {
			for(int i = 0; i < quad.vertices.length / 2; i++) {
				Vertex temp = quad.vertices[i];
				int i2 = quad.vertices.length - i - 1;
				quad.vertices[i] = quad.vertices[i2];
				quad.vertices[i2] = temp;
			}
			return new Quad<>(quad.vertices, quad.normal.negated(), quad.faceData);
		} else return quad;
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
