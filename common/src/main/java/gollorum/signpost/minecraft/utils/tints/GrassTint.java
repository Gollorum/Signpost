package gollorum.signpost.minecraft.utils.tints;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.BlockAndTintGetter;

public class GrassTint implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return BiomeColors.getAverageGrassColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("grass", new Serializer(GrassTint.class, CODEC, STREAM_CODEC));
    }

    public static final MapCodec<GrassTint> CODEC = MapCodec.unit(new GrassTint());

    public static final StreamCodec<ByteBuf, GrassTint> STREAM_CODEC = StreamCodec.unit(new GrassTint());

}
