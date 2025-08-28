package gollorum.signpost.minecraft.models.modelGeneration;

import org.joml.Vector3f;

public record Quad(Vertex[] vertices, Vector3f normal) {
    public record Vertex(Vector3f pos, float u, float v) {}
}