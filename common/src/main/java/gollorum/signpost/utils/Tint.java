package gollorum.signpost.utils;

import gollorum.signpost.minecraft.utils.tints.*;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.HashMap;
import java.util.Map;

public interface Tint {

    int getColorAt(BlockAndTintGetter level, BlockPos pos);

    record Serializer(
        CompoundSerializable<? extends Tint> compound,
        BufferSerializable<? extends Tint> buffer
    ) { }

    public static class Serialization {

        private static final Map<String, Serializer> allSerializers = new HashMap<>();
        public static void register(String type, Serializer serializer) {
            allSerializers.put(type, serializer);
        }

        static {
            FoliageTint.register();
            BlockColorTint.register();
            GrassTint.register();
            FluidTint.register();
        }

        public static final CompoundSerializable<Tint> COMPOUND = new CompoundSerializable<>() {

            @Override
            public void encode(CompoundTag compound, Tint tint, HolderLookup.Provider provider) {
                for (var e : allSerializers.entrySet()) {
                    if (tint.getClass() == e.getValue().buffer().getTargetClass()) {
                        compound.putString("Type", e.getKey());
                        ((CompoundSerializable<Tint>) e.getValue().compound()).encode(compound, tint, provider);
                        return;
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
                return allSerializers.get(compound.getString("Type")).compound().decode(compound, provider);
            }
        };

        public static final BufferSerializable<Tint> BUFFER = new BufferSerializable<>() {

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, Tint tint) {
                for (var e : allSerializers.entrySet()) {
                    if(tint.getClass() == e.getValue().buffer().getTargetClass()) {
                        StringSerializer.Buffer.encode(buffer, e.getKey());
                        ((BufferSerializable<Tint>)e.getValue().buffer()).encode(buffer, tint);
                        return;
                    }
                }
                throw new RuntimeException("Failed to serialize tint type " + tint.getClass());
            }

            @Override
            public Tint decode(RegistryFriendlyByteBuf buffer) {
                return allSerializers.get(StringSerializer.Buffer.decode(buffer)).buffer().decode(buffer);
            }

            @Override
            public Class<Tint> getTargetClass() {
                return Tint.class;
            }

        };
    }

}
