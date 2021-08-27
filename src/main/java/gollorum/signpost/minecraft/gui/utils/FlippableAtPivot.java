package gollorum.signpost.minecraft.gui.utils;

public class FlippableAtPivot implements Flippable {

    private final WithMutableX wrapped;
    private final int pivotX;
    private boolean isFlipped;

    public FlippableAtPivot(WithMutableX wrapped, int pivotX) {
        this.wrapped = wrapped;
        this.pivotX = pivotX;
    }

    @Override
    public boolean isFlipped() {
        return isFlipped;
    }

    @Override
    public void setFlipped(boolean isFlipped) {
        if(isFlipped != this.isFlipped) {
            this.isFlipped = isFlipped;
            wrapped.setX(pivotX + pivotX - (wrapped.getX() + wrapped.width()));
        }
    }
}
