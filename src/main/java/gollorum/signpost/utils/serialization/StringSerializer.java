package gollorum.signpost.utils.serialization;

import net.minecraft.network.FriendlyByteBuf;

public final class StringSerializer implements BufferSerializable<String> {

    public static final StringSerializer instance = new StringSerializer();

    @Override
    public Class<String> getTargetClass() {
        return String.class;
    }

    @Override
    public void write(String s, FriendlyByteBuf buffer) {
        buffer.writeUtf(s);
    }

    @Override
    public String read(FriendlyByteBuf buffer) {
        return buffer.readUtf();
    }
}
