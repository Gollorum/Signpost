package gollorum.signpost.utils.modelGeneration;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;

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
			quads.computeIfAbsent(new RenderMaterial(PlayerContainer.LOCATION_BLOCKS_TEXTURE, q.faceData.texture), k -> new ArrayList<>())
				.add(quad);
		}
	}

	public void render(MatrixStack.Entry matrixEntry, IRenderTypeBuffer buffer, RenderType renderType, int packedLight, int packedOverlay, float r, float g, float b) {
		Matrix4f matrix4f = matrixEntry.getMatrix();
		Matrix3f matrixNormal = matrixEntry.getNormal();

		for(Map.Entry<RenderMaterial, List<Quad>> entry : quads.entrySet()) {
			for(Quad quad : entry.getValue()) {
				Vector3f normal = quad.normal.copy();
				normal.transform(matrixNormal);
				float normalX = normal.getX();
				float normalY = normal.getY();
				float normalZ = normal.getZ();

				IVertexBuilder vertexBuilder = entry.getKey().getBuffer(buffer, x -> renderType);

				for(Quad.Vertex vertex: quad.vertices) {
					Vector4f pos = new Vector4f(vertex.pos.getX(), vertex.pos.getY(), vertex.pos.getZ(), 1.0F);
					pos.transform(matrix4f);
					vertexBuilder.addVertex(
						pos.getX(), pos.getY(), pos.getZ(),
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
					case Clockwise90:
						this.u = 1 - v;
						this.v = u;
						break;
					case CounterClockwise90:
						this.u = v;
						this.v = 1 - u;
						break;
					case UpsideDown:
						this.u = 1 - u;
						this.v = 1 - v;
						break;
					case Zero:
					default:
						this.u = u;
						this.v = v;
						break;
				}
			}
		}

	}

}
