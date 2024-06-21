package gollorum.signpost.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ModelWaystoneImpl extends ModelWaystone {

    public ModelWaystoneImpl(Variant variant) {
        super(variant);
    }

    private ModelWaystoneImpl(Variant variant, Properties properties) {
        super(variant, properties);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return WaystoneBlock.fillClonedItemStack(super.getCloneItemStack(state, target, level, pos, player), level, pos, player);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return RecordCodecBuilder.mapCodec((builder) -> builder.group(
            propertiesCodec(),
            Codec.STRING.fieldOf("variant").forGetter(block -> ((ModelWaystone)block).variant.name)
        ).apply(builder, (properties, variantName) ->
            ModelWaystone.variants.stream().filter(v -> Objects.equals(v.name, variantName)).findAny().orElseThrow().createBlock(v -> new ModelWaystoneImpl(
                v,
                properties
            ))
        ));
    }
}
