package gollorum.signpost.minecraft.gui.utils;

public interface Flippable {
    boolean isFlipped();
    void setFlipped(boolean isFlipped);
    default void flip() {
        setFlipped(!isFlipped());
    }
}
