package gollorum.signpost.minecraft.utils.tints;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.utils.Tint;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.BlockAndTintGetter;

public class FoliageTint implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return BiomeColors.getAverageFoliageColor(level, pos);
    }

    public static void register() {
        Tint.Serialization.register("foliage", new Serializer(FoliageTint.class, CODEC, STREAM_CODEC));
    }

    public static final MapCodec<FoliageTint> CODEC = MapCodec.unit(new FoliageTint());

    public static final StreamCodec<ByteBuf, FoliageTint> STREAM_CODEC = StreamCodec.unit(new FoliageTint());

}
