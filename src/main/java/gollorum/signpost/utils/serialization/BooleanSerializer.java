package gollorum.signpost.utils.serialization;

import net.minecraft.network.PacketBuffer;

public final class BooleanSerializer implements BufferSerializable<Boolean> {

    public static final BooleanSerializer instance = new BooleanSerializer();

    @Override
    public Class<Boolean> getTargetClass() {
        return Boolean.class;
    }

    @Override
    public void write(Boolean aBoolean, PacketBuffer buffer) {
        buffer.writeBoolean(aBoolean);
    }

    @Override
    public Boolean read(PacketBuffer buffer) {
        return buffer.readBoolean();
    }
}
