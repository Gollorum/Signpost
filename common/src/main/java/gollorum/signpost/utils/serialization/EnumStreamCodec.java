package gollorum.signpost.utils.serialization;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class EnumStreamCodec<T extends Enum<T>> implements StreamCodec<FriendlyByteBuf, T> {

    private final Class<T> tClass;

    public EnumStreamCodec(Class<T> tClass) {
        this.tClass = tClass;
    }

    public static <T extends Enum<T>> EnumStreamCodec<T> of(Class<T> tClass) {
        return new EnumStreamCodec<>(tClass);
    }

    @Override
    public T decode(FriendlyByteBuf b) {
        return b.readEnum(tClass);
    }

    @Override
    public void encode(FriendlyByteBuf b, T t) {
        b.writeEnum(t);
    }
}
