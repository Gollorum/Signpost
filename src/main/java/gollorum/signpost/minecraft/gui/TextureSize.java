package gollorum.signpost.minecraft.gui;

public final class TextureSize {

    public final int width;
    public final int height;

    public TextureSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public TextureSize add(TextureSize other) {
        return new TextureSize(width + other.width, height + other.height);
    }

    public TextureSize subtract(TextureSize other) {
        return new TextureSize(width - other.width, height - other.height);
    }

    public TextureSize scale(float scale) {
        return new TextureSize((int)(width * scale), (int) (height * scale));
    }
}
