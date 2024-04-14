package gollorum.signpost.utils.serialization;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;

public interface CompoundSerializable<T> extends BufferSerializable<T> {

    CompoundTag write(T t, CompoundTag compound);

    default CompoundTag write(T t){
        CompoundTag ret = new CompoundTag();
        write(t, ret);
        return ret;
    }

    boolean isContainedIn(CompoundTag compound);

    T read(CompoundTag compound);

    @Override
    default void write(T t, FriendlyByteBuf buffer){ StringSerializer.instance.write(write(t).toString(), buffer); }

    @Override
    default T read(FriendlyByteBuf buffer){
        try {
            return read(TagParser.parseTag(StringSerializer.instance.read(buffer)));
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    default OptionalSerializer<T> optional() { return OptionalSerializer.from(this); }
}
