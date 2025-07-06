package gollorum.signpost.utils.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class BlockPosSerializer {

    public static final Codec<BlockPos> CODEC = RecordCodecBuilder.create(i -> i.group(
        com.mojang.serialization.Codec.INT.fieldOf("X").forGetter(BlockPos::getX),
        com.mojang.serialization.Codec.INT.fieldOf("Y").forGetter(BlockPos::getY),
        com.mojang.serialization.Codec.INT.fieldOf("Z").forGetter(BlockPos::getZ)
    ).apply(i, BlockPos::new));

}
