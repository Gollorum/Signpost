package gollorum.signpost.utils.serialization;

import net.minecraft.network.RegistryFriendlyByteBuf;

public final class IntSerializer implements BufferSerializable<Integer> {

    public static final IntSerializer instance = new IntSerializer();

    @Override
    public Class<Integer> getTargetClass() {
        return Integer.class;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, Integer integer) {
        buffer.writeInt(integer);
    }

    @Override
    public Integer decode(RegistryFriendlyByteBuf buffer) {
        return buffer.readInt();
    }
}
