package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import gollorum.signpost.minecraft.utils.tints.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.HashMap;
import java.util.Map;

public interface Tint {

    int getColorAt(BlockAndTintGetter level, BlockPos pos);

    record Serializer(
        Class<? extends Tint> targetClass,
        MapCodec<? extends Tint> codec,
        StreamCodec<ByteBuf, ? extends Tint> streamCodec
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

        public static final Codec<Tint> CODEC = Codec.STRING.dispatch("Type",
            tint -> {
                for (var e : allSerializers.entrySet()) {
                    if (tint.getClass() == e.getValue().targetClass()) {
                        return e.getKey();
                    }
                }
                throw new RuntimeException("Failed to serialize tint type " + tint.getClass());
            }, type -> {
                if (allSerializers.containsKey(type)) {
                    return allSerializers.get(type).codec();
                } else {
                    throw new RuntimeException("Unknown tint type: " + type);
                }
            });

        public static final StreamCodec<ByteBuf, Tint> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.dispatch(
            tint -> {
                for (var e : allSerializers.entrySet()) {
                    if (tint.getClass() == e.getValue().targetClass()) {
                        return e.getKey();
                    }
                }
                throw new RuntimeException("Failed to serialize tint type " + tint.getClass());
            },
            type -> {
                if (allSerializers.containsKey(type)) {
                    return allSerializers.get(type).streamCodec();
                } else {
                    throw new RuntimeException("Unknown tint type: " + type);
                }
            }
        );
    }

}
