package gollorum.signpost.block;

import com.mojang.datafixers.types.Type;
import gollorum.signpost.minecraft.block.tiles.IBlockEntityTypeFactory;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

public class BlockEntityTypeFactory implements IBlockEntityTypeFactory {
    @Override
    public <T extends BlockEntity> BlockEntityType<T> create(BiFunction<BlockPos, BlockState, T> factory, Block[] blocks, Type<?> type) {
        return FabricBlockEntityTypeBuilder.create(factory::apply, blocks).build();
    }
}
