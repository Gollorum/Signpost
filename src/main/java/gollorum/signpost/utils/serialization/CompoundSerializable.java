package gollorum.signpost.utils.serialization;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.PacketBuffer;

public interface CompoundSerializable<T> extends BufferSerializable<T> {

    /// Dangerous if keys are duplicate. Use with caution.
    void writeTo(T t, CompoundNBT compound, String keyPrefix);
    default void writeTo(T t, CompoundNBT compound){
        writeTo(t, compound, "");
    }

    default CompoundNBT write(T t){
        CompoundNBT ret = new CompoundNBT();
        writeTo(t, ret);
        return ret;
    }

    boolean isContainedIn(CompoundNBT compound, String keyPrefix);
    default boolean isContainedIn(CompoundNBT compound) { return isContainedIn(compound, ""); }

    T read(CompoundNBT compound, String keyPrefix);

    default T read(CompoundNBT compound){ return read(compound, ""); }

    @Override
    default void writeTo(T t, PacketBuffer buffer){ buffer.writeString(write(t).toString()); }

    @Override
    default T readFrom(PacketBuffer buffer){
        try {
            return read(JsonToNBT.getTagFromJson(buffer.readString(32767)));
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
