package gollorum.signpost.utils;

import gollorum.signpost.minecraft.utils.tints.*;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.core.BlockPos;
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
        public CompoundTag write(Tint tint, CompoundTag compound) {
            for (var e : allSerializers.entrySet()) {
                if(tint.getClass() == e.getValue().getTargetClass()) {
                    compound.putString("Type", e.getKey());
                    ((CompoundSerializable<Tint>)e.getValue()).write(tint, compound);
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
        public Tint read(CompoundTag compound) {
            return allSerializers.get(compound.getString("Type")).read(compound);
        }

        @Override
        public void write(Tint tint, FriendlyByteBuf buffer) {
            for (var e : allSerializers.entrySet()) {
                if(tint.getClass() == e.getValue().getTargetClass()) {
                    StringSerializer.instance.write(e.getKey(), buffer);
                    ((CompoundSerializable<Tint>)e.getValue()).write(tint, buffer);
                    return;
                }
            }
            throw new RuntimeException("Failed to serialize tint type " + tint.getClass());
        }

        @Override
        public Tint read(FriendlyByteBuf buffer) {
            return allSerializers.get(StringSerializer.instance.read(buffer)).read(buffer);
        }

        @Override
        public Class<Tint> getTargetClass() {
            return Tint.class;
        }

    }

}
