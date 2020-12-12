package gollorum.signpost.minecraft.gui;

public interface Flippable {
    boolean isFlipped();
    void setFlipped(boolean isFlipped);
    default void flip() {
        setFlipped(!isFlipped());
    }
}
