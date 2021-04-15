package gollorum.signpost.utils.serialization;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.PacketBuffer;

public interface CompoundSerializable<T> extends BufferSerializable<T> {

    CompoundNBT write(T t, CompoundNBT compound);

    default CompoundNBT write(T t){
        CompoundNBT ret = new CompoundNBT();
        write(t, ret);
        return ret;
    }

    boolean isContainedIn(CompoundNBT compound);

    T read(CompoundNBT compound);

    @Override
    default void write(T t, PacketBuffer buffer){ buffer.writeString(write(t).toString()); }

    @Override
    default T read(PacketBuffer buffer){
        try {
            return read(JsonToNBT.getTagFromJson(buffer.readString(32767)));
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    default OptionalSerializer<T> optional() { return OptionalSerializer.from(this); }
}
