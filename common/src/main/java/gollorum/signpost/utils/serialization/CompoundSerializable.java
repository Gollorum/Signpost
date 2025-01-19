package gollorum.signpost.utils.serialization;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface CompoundSerializable<T> {

    default CompoundTag encode(T t, HolderLookup.Provider provider){
        CompoundTag ret = new CompoundTag();
        encode(ret, t, provider);
        return ret;
    }

    void encode(CompoundTag compound, T t, HolderLookup.Provider provider);

    T decode(CompoundTag compound, HolderLookup.Provider provider);

    boolean isContainedIn(CompoundTag compound);

    default OptionalCompoundSerializer<T> optional() { return OptionalCompoundSerializer.from(this); }

}
