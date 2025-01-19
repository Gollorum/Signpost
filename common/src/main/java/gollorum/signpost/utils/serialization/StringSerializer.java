package gollorum.signpost.utils.serialization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class StringSerializer {

    public static final CompoundSerializable<String> Compound = new Compound();
    public static final BufferSerializable<String> Buffer = new Buffer();

    private static final class Compound implements CompoundSerializable<String> {

        @Override
        public void encode(CompoundTag compound, String s, HolderLookup.Provider provider) {
            compound.putString("String", s);
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("String");
        }

        @Override
        public String decode(CompoundTag compound, HolderLookup.Provider provider) {
            return compound.getString("String");
        }
    }

    public static final class Buffer implements BufferSerializable<String> {

        @Override
        public Class<String> getTargetClass() {
            return String.class;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, String s) {
            buffer.writeUtf(s);
        }

        @Override
        public String decode(RegistryFriendlyByteBuf buffer) {
            return buffer.readUtf();
        }
    }

}
