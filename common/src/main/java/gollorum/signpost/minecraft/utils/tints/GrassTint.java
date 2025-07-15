package gollorum.signpost.minecraft.utils.tints;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.utils.Tint;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.BlockAndTintGetter;

public class GrassTint implements Tint {
    
    public static final GrassTint INSTANCE = new GrassTint();

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return BiomeColors.getAverageGrassColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("grass", new Serializer(GrassTint.class, CODEC, STREAM_CODEC));
    }

    public static final MapCodec<GrassTint> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<ByteBuf, GrassTint> STREAM_CODEC = StreamCodec.unit(INSTANCE);

}
