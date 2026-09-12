package gollorum.signpost.minecraft.utils.tints;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.Tint;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;

import java.util.Objects;

public record BlockColorTint(Block block, int tintIndex) implements Tint {

    @Override
    public int getColorAt(BlockAndTintGetter level, BlockPos pos) {
        return net.minecraft.client.Minecraft.getInstance().getBlockColors().getColor(block.defaultBlockState(), level, pos, tintIndex);
    }

    public static void register() {
        Tint.Serialization.register("blockColor", new Serializer(BlockColorTint.class, CODEC, STREAM_CODEC));
    }

    private static Registry<Block> getBlockRegistry() {
        return BuiltInRegistries.BLOCK;
    }

    private static Block getBlock(ResourceLocation key) {
        return getBlockRegistry().get(key);
    }

    private static ResourceLocation getKey(Block block) {
        return Objects.requireNonNull(getBlockRegistry().getKey(block));
    }

    public static final MapCodec<BlockColorTint> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.fieldOf("Block").xmap(BlockColorTint::getBlock, BlockColorTint::getKey).forGetter(BlockColorTint::block),
        Codec.INT.fieldOf("TintIndex").forGetter(BlockColorTint::tintIndex)
    ).apply(i, BlockColorTint::new));


    public static final StreamCodec<ByteBuf, BlockColorTint> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, t -> getKey(t.block),
        ByteBufCodecs.INT, t -> t.tintIndex,
        (blockKey, tintIndex) -> new BlockColorTint(getBlock(blockKey), tintIndex)
    );

}
