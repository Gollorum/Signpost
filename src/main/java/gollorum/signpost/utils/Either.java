package gollorum.signpost.utils;

import java.util.Objects;
import java.util.function.Function;

/// Not a monad
public abstract class Either<Left, Right> {

    public static <Left, Right> Either<Left, Right> left(Left left) { return new LeftImpl<>(left); }
    public static <Left, Right> Either<Left, Right> right(Right right) { return new RightImpl<>(right); }

    private Either() { }

    public abstract boolean isLeft();

    public abstract Right rightOr(Function<Left, Right> func);
    public abstract Left leftOr(Function<Right, Left> func);

    public abstract <NewRight> Either<Left, NewRight> mapRight(Function<Right, NewRight> mapping);
    public abstract <NewLeft> Either<NewLeft, Right> mapLeft(Function<Left, NewLeft> mapping);

    public Right rightOrThrow() { return rightOr(l -> { throw new RuntimeException("Right value was not present."); }); }

    public Left leftOrThrow() { return leftOr(l -> { throw new RuntimeException("Left value was not present."); }); }

    private static class LeftImpl<Left, Right> extends Either<Left, Right> {
        private final Left left;
        private LeftImpl(Left left) { this.left = left; }

        @Override
        public boolean isLeft() { return true; }

        @Override
        public Right rightOr(Function<Left, Right> func) { return func.apply(left); }

        @Override
        public Left leftOr(Function<Right, Left> func) { return left; }

        @Override
        public <NewRight> Either<Left, NewRight> mapRight(Function<Right, NewRight> mapping) {
            return Either.left(left);
        }

        @Override
        public <NewLeft> Either<NewLeft, Right> mapLeft(Function<Left, NewLeft> mapping) {
            return Either.left(mapping.apply(left));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LeftImpl<?, ?> left1 = (LeftImpl<?, ?>) o;
            return Objects.equals(left, left1.left);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left);
        }
    }

    private static class RightImpl<Left, Right> extends Either<Left, Right> {
        private final Right right;
        private RightImpl(Right right) { this.right = right; }

        @Override
        public boolean isLeft() { return true; }

        @Override
        public Right rightOr(Function<Left, Right> func) { return right; }

        @Override
        public Left leftOr(Function<Right, Left> func) { return func.apply(right); }

        @Override
        public <NewRight> Either<Left, NewRight> mapRight(Function<Right, NewRight> mapping) {
            return Either.right(mapping.apply(right));
        }

        @Override
        public <NewLeft> Either<NewLeft, Right> mapLeft(Function<Left, NewLeft> mapping) {
            return Either.right(right);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RightImpl<?, ?> right1 = (RightImpl<?, ?>) o;
            return Objects.equals(right, right1.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(right);
        }
    }

}
