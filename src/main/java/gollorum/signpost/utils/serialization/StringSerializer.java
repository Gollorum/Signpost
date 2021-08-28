package gollorum.signpost.utils.serialization;

import net.minecraft.network.PacketBuffer;

public final class StringSerializer implements BufferSerializable<String> {

    public static final StringSerializer instance = new StringSerializer();

    @Override
    public Class<String> getTargetClass() {
        return String.class;
    }

    @Override
    public void write(String s, PacketBuffer buffer) {
        buffer.writeUtf(s);
    }

    @Override
    public String read(PacketBuffer buffer) {
        return buffer.readUtf();
    }
}
