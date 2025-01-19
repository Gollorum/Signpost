package gollorum.signpost.utils;

import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/// Not a monad
public abstract class Either<Left, Right> {

    public static <Left, Right> Either<Left, Right> left(Left left) { return new LeftImpl<>(left); }
    public static <Left, Right> Either<Left, Right> right(Right right) { return new RightImpl<>(right); }

    public static <Left, Right> Either<Left, Right> leftIfPresent(Optional<Left> left, Supplier<Right> right) {
        return left.map(Either::<Left, Right>left).orElseGet(() -> right(right.get()));
    }
    public static <Left, Right> Either<Left, Right> rightIfPresent(Optional<Right> right, Supplier<Left> left) {
        return right.map(Either::<Left, Right>right).orElseGet(() -> left(left.get()));
    }

    private Either() { }

    public abstract boolean isLeft();
    public boolean isRight() { return !isLeft(); }

    public Either<Right, Left> flip() {
        return match(Either::right, Either::left);
    }

    public abstract Right rightOr(Function<Left, Right> func);
    public abstract Left leftOr(Function<Right, Left> func);

    public abstract <NewRight> Either<Left, NewRight> flatMapRight(Function<Right, Either<Left, NewRight>> mapping);
    public abstract <NewLeft> Either<NewLeft, Right> flatMapLeft(Function<Left, Either<NewLeft, Right>> mapping);

    public final <NewRight> Either<Left, NewRight> mapRight(Function<Right, NewRight> mapping) {
        return flatMapRight(r -> Either.right(mapping.apply(r)));
    }
    public final <NewLeft> Either<NewLeft, Right> mapLeft(Function<Left, NewLeft> mapping) {
        return flatMapLeft(l -> Either.left(mapping.apply(l)));
    }

    public abstract <Out> Out match(Function<Left, Out> leftMapping, Function<Right, Out> rightMapping);

    public abstract void consume(Consumer<Left> leftMapping, Consumer<Right> rightMapping);

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
        public <NewRight> Either<Left, NewRight> flatMapRight(Function<Right, Either<Left, NewRight>> mapping) {
            return Either.left(left);
        }

        @Override
        public <NewLeft> Either<NewLeft, Right> flatMapLeft(Function<Left, Either<NewLeft, Right>> mapping) {
            return mapping.apply(left);
        }

        @Override
        public <Out> Out match(Function<Left, Out> leftMapping, Function<Right, Out> rightMapping) {
            return leftMapping.apply(left);
        }

        @Override
        public void consume(Consumer<Left> leftMapping, Consumer<Right> rightMapping) {
            leftMapping.accept(left);
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

        @Override
        public String toString() {
            return "Left{" + left + '}';
        }
    }

    private static class RightImpl<Left, Right> extends Either<Left, Right> {
        private final Right right;
        private RightImpl(Right right) { this.right = right; }

        @Override
        public boolean isLeft() { return false; }

        @Override
        public Right rightOr(Function<Left, Right> func) { return right; }

        @Override
        public Left leftOr(Function<Right, Left> func) { return func.apply(right); }

        @Override
        public <NewRight> Either<Left, NewRight> flatMapRight(Function<Right, Either<Left, NewRight>> mapping) {
            return mapping.apply(right);
        }

        @Override
        public <NewLeft> Either<NewLeft, Right> flatMapLeft(Function<Left, Either<NewLeft, Right>> mapping) {
            return Either.right(right);
        }

        @Override
        public <Out> Out match(Function<Left, Out> leftMapping, Function<Right, Out> rightMapping) {
            return rightMapping.apply(right);
        }

        @Override
        public void consume(Consumer<Left> leftMapping, Consumer<Right> rightMapping) {
            rightMapping.accept(right);
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

        @Override
        public String toString() {
            return "Right{" + right + '}';
        }
    }

    public static class BufferSerializer<Left, Right> implements BufferSerializable<Either<Left, Right>> {

        private final BufferSerializable<Left> leftS;
        private final BufferSerializable<Right> rightS;

        public BufferSerializer(BufferSerializable<Left> leftS, BufferSerializable<Right> rightS) {
            this.leftS = leftS;
            this.rightS = rightS;
        }

        public static <L, R> BufferSerializer<L, R> of(BufferSerializable<L> ls, BufferSerializable<R> rs) {
            return new BufferSerializer<>(ls, rs);
        }

        @Override
        public Class<Either<Left, Right>> getTargetClass() {
            return (Class<Either<Left, Right>>) Either.<Left, Right>left(null).getClass();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, Either<Left, Right> leftRightEither) {
            buffer.writeBoolean(leftRightEither.isLeft());
            leftRightEither.consume(
                l -> leftS.encode(buffer, l),
                r -> rightS.encode(buffer, r)
            );
        }

        @Override
        public Either<Left, Right> decode(RegistryFriendlyByteBuf buffer) {
            return buffer.readBoolean()
                ? Either.left(leftS.decode(buffer))
                : Either.right(rightS.decode(buffer));
        }
    }

    public static final class Serializer<Left, Right> implements CompoundSerializable<Either<Left, Right>> {

        private final CompoundSerializable<Left> leftS;
        private final CompoundSerializable<Right> rightS;

        public Serializer(CompoundSerializable<Left> leftS, CompoundSerializable<Right> rightS) {
            this.leftS = leftS;
            this.rightS = rightS;
        }

        public static <L, R> Serializer<L, R> of(CompoundSerializable<L> ls, CompoundSerializable<R> rs) {
            return new Serializer<>(ls, rs);
        }

        @Override
        public void encode(CompoundTag compound, Either<Left, Right> leftRightEither, HolderLookup.Provider provider) {
            compound.putBoolean("IsLeft", leftRightEither.isLeft());
            compound.put("Data", leftRightEither.match(t -> leftS.encode(t, provider), t1 -> rightS.encode(t1, provider)));
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("IsLeft") && compound.contains("Data") &&
                compound.getBoolean("IsLeft")
                    ? leftS.isContainedIn(compound.getCompound("Data"))
                    : rightS.isContainedIn(compound.getCompound("Data"));
        }

        @Override
        public Either<Left, Right> decode(CompoundTag compound, HolderLookup.Provider provider) {
            return compound.getBoolean("IsLeft")
                ? Either.left(leftS.decode(compound.getCompound("Data"), provider))
                : Either.right(rightS.decode(compound.getCompound("Data"), provider));
        }

    }

}
