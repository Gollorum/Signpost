package gollorum.signpost.utils.modelGeneration;

public enum FaceRotation {
    Zero(),
    Clockwise90(),
    CounterClockwise90(),
    UpsideDown();

    public FaceRotation inverse() {
        return switch (this) {
            case Zero -> Zero;
            case Clockwise90 -> CounterClockwise90;
            case CounterClockwise90 -> Clockwise90;
            case UpsideDown -> UpsideDown;
        };
    }

    public FaceRotation rotate180() {
        return switch (this) {
            case Zero -> UpsideDown;
            case Clockwise90 -> CounterClockwise90;
            case CounterClockwise90 -> Clockwise90;
            case UpsideDown -> Zero;
        };
    }

}
