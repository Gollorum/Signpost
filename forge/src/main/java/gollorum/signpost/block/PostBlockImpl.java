package gollorum.signpost.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.data.PostData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PostBlockImpl extends PostBlock {

    public PostBlockImpl(Properties properties, ModelType modelType, Variant variant) {
        super(properties, modelType, variant);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack ret = super.getCloneItemStack(level,  pos, state, includeData);
        if (!includeData) return ret;
        level.getBlockEntity(pos, PostTile.getBlockEntityType()).ifPresent(tile -> {
            var data = new PostData(tile.parts());
            ret.applyComponents(DataComponentPatch.builder().set(PostData.TYPE, data).build());
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
