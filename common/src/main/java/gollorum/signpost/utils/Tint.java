package gollorum.signpost.utils;

import gollorum.signpost.minecraft.utils.tints.*;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.HashMap;
import java.util.Map;

public interface Tint {

    int getColorAt(BlockAndTintGetter level, BlockPos pos);

    public static class Serialization implements CompoundSerializable<Tint> {

        private static final Map<String, CompoundSerializable<? extends Tint>> allSerializers = new HashMap<>();
        public static void register(String type, CompoundSerializable<? extends Tint> serializer) {
            allSerializers.put(type, serializer);
        }

        static {
            FoliageTint.register();
            BlockColorTint.register();
            GrassTint.register();
            FluidTint.register();
        }

        public static final Serialization instance = new Serialization();

        @Override
        public void encode(CompoundTag compound, Tint tint, HolderLookup.Provider provider) {
            for (var e : allSerializers.entrySet()) {
                if(tint.getClass() == e.getValue().getTargetClass()) {
                    compound.putString("Type", e.getKey());
                    ((CompoundSerializable<Tint>)e.getValue()).encode(compound, tint, );
                    return compound;
                }
            }
            throw new RuntimeException("Failed to serialize tint type " + tint.getClass());
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("Type") && allSerializers.containsKey(compound.getString("Type"));
        }

        @Override
        public Tint decode(CompoundTag compound, HolderLookup.Provider provider) {
            return allSerializers.get(compound.getString("Type")).decode(compound, );
        }

        @Override
        public void encode(FriendlyByteBuf buffer, Tint tint) {
            for (var e : allSerializers.entrySet()) {
                if(tint.getClass() == e.getValue().getTargetClass()) {
                    StringSerializer.instance.encode(buffer, e.getKey());
                    ((CompoundSerializable<Tint>)e.getValue()).encode(buffer, tint, );
                    return;
                }
            }
            throw new RuntimeException("Failed to serialize tint type " + tint.getClass());
        }

        @Override
        public Tint decode(FriendlyByteBuf buffer) {
            return allSerializers.get(StringSerializer.instance.decode(buffer)).decode(buffer, );
        }

        @Override
        public Class<Tint> getTargetClass() {
            return Tint.class;
        }

    }

}
