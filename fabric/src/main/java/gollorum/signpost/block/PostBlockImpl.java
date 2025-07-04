package gollorum.signpost.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PostBlockImpl extends PostBlock {

    public PostBlockImpl(Properties properties, ModelType modelType, Variant variant) {
        super(properties, modelType, variant);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack ret = super.getCloneItemStack(level, pos, state, includeData);
        level.getBlockEntity(pos, PostTile.getBlockEntityType()).ifPresent(tile -> {
            if(!ret.hasTag()) ret.setTag(new CompoundTag());
            ret.getTag().put("Parts", tile.writeParts(false));
        });
        return ret;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return RecordCodecBuilder.mapCodec((builder) -> builder.group(
            Codec.STRING.fieldOf("variant").forGetter(block -> ((PostBlock)block).type.name)
        ).apply(builder, variantName ->
            AllVariants.stream().filter(v -> Objects.equals(v.type.name, variantName)).findAny().orElseThrow().createBlock(PostBlockImpl::new)
        ));
    }

}
