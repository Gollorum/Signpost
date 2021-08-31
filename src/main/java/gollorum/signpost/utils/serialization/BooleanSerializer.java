package gollorum.signpost.utils.serialization;

import net.minecraft.network.FriendlyByteBuf;

public final class BooleanSerializer implements BufferSerializable<Boolean> {

    public static final BooleanSerializer instance = new BooleanSerializer();

    @Override
    public Class<Boolean> getTargetClass() {
        return Boolean.class;
    }

    @Override
    public void write(Boolean aBoolean, FriendlyByteBuf buffer) {
        buffer.writeBoolean(aBoolean);
    }

    @Override
    public Boolean read(FriendlyByteBuf buffer) {
        return buffer.readBoolean();
    }
}
