package gollorum.signpost.utils.serialization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class StringSerializer implements CompoundSerializable<String> {

    public static final StringSerializer instance = new StringSerializer();

    @Override
    public Class<String> getTargetClass() {
        return String.class;
    }

    @Override
    public CompoundTag write(String s, CompoundTag compound) {
        compound.putString("String", s);
        return compound;
    }

    @Override
    public boolean isContainedIn(CompoundTag compound) {
        return compound.contains("String");
    }

    @Override
    public String read(CompoundTag compound) {
        return compound.getString("String");
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
