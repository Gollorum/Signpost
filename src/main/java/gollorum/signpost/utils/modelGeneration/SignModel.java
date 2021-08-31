package gollorum.signpost.utils.modelGeneration;

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class SignModel {

	private final Map<RenderMaterial, List<Quad>> quads = new HashMap<>();

	private static final float pixelToWorld = 1 / 16f;
	public void addCube(Cube<ResourceLocation> cube) {
		for(Cube.Quad<ResourceLocation> q: cube.getQuads()) {
			Quad quad = new Quad(
				q.normal.asVec3f(),
				Arrays.stream(q.vertices).map(v -> new Quad.Vertex(
					v.pos.mul(pixelToWorld).asVec3f(),
					v.u * pixelToWorld,
					v.v * pixelToWorld,
					q.faceData.rotation
				)).toArray(Quad.Vertex[]::new)
			);
			quads.computeIfAbsent(new RenderMaterial(PlayerContainer.BLOCK_ATLAS, q.faceData.texture), k -> new ArrayList<>())
				.add(quad);
		}
	}

	public void render(MatrixStack.Entry matrixEntry, IRenderTypeBuffer buffer, RenderType renderType, int packedLight, int packedOverlay, float r, float g, float b) {
		Matrix4f matrix4f = matrixEntry.pose();
		Matrix3f matrixNormal = matrixEntry.normal();

		for(Map.Entry<RenderMaterial, List<Quad>> entry : quads.entrySet()) {
			for(Quad quad : entry.getValue()) {
				Vector3f normal = quad.normal.copy();
				normal.transform(matrixNormal);
				float normalX = normal.x();
				float normalY = normal.y();
				float normalZ = normal.z();

				IVertexBuilder vertexBuilder = entry.getKey().buffer(buffer, x -> renderType);

				for(Quad.Vertex vertex: quad.vertices) {
					Vector4f pos = new Vector4f(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), 1.0F);
					pos.transform(matrix4f);
					vertexBuilder.vertex(
						pos.x(), pos.y(), pos.z(),
						r, g, b, 1,
						vertex.u,
						vertex.v,
						packedOverlay,
						packedLight,
						normalX, normalY, normalZ
	                );
				}
			}
		}
	}

	private static class Quad {

		private Quad(Vector3f normal, Vertex[] vertices) {
			this.normal = normal;
			this.vertices = vertices;
		}

		public final Vector3f normal;
		public final Vertex[] vertices;

		public static class Vertex {
			public final Vector3f pos;
			public final float u;
			public final float v;

			public Vertex(Vector3f pos, float u, float v, FaceRotation rotation) {
				this.pos = pos;
				switch (rotation) {
					case Clockwise90 -> {
						this.u = 1 - v;
						this.v = u;
					}
					case CounterClockwise90 -> {
						this.u = v;
						this.v = 1 - u;
					}
					case UpsideDown -> {
						this.u = 1 - u;
						this.v = 1 - v;
					}
					default -> {
						this.u = u;
						this.v = v;
					}
				}
			}
		}

	}

}
