package gollorum.signpost.utils.serialization;

import net.minecraft.network.RegistryFriendlyByteBuf;

public final class BooleanSerializer implements BufferSerializable<Boolean> {

    public static final BooleanSerializer instance = new BooleanSerializer();

    @Override
    public Class<Boolean> getTargetClass() {
        return Boolean.class;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer, Boolean aBoolean) {
        buffer.writeBoolean(aBoolean);
    }

    @Override
    public Boolean decode(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean();
    }
}
