package gollorum.signpost.minecraft.models.modelGeneration;

import org.joml.Vector2f;
import org.joml.Vector3f;

public record Quad(Vertex[] vertices, Vector3f normal) {
    public record Vertex(Vector3f pos, float u, float v) {
        public Vertex(Vector3f pos, Vector2f uv) {
            this(pos, uv.x, uv.y);
        }
    }
}