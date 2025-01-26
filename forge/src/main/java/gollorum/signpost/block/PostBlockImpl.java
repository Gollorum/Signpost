package gollorum.signpost.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PostBlockImpl extends PostBlock {

    public PostBlockImpl(Properties properties, ModelType modelType) {
        super(properties, modelType);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack ret = super.getCloneItemStack(state, target, level, pos, player);
        level.getBlockEntity(pos, PostTile.getBlockEntityType()).ifPresent(tile -> {
            var component = ret.getComponents().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            component.put("Parts", tile.writeParts(false, level.registryAccess()));
            ret.applyComponents(DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(component)).build());
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
