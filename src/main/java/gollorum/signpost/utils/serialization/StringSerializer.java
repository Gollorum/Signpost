package gollorum.signpost.utils.serialization;

import net.minecraft.network.PacketBuffer;

public final class StringSerializer implements BufferSerializable<String> {
    @Override
    public Class<String> getTargetClass() {
        return String.class;
    }

    @Override
    public void write(String s, PacketBuffer buffer) {
        buffer.writeString(s);
    }

    @Override
    public String read(PacketBuffer buffer) {
        return buffer.readString(32767);
    }
}
