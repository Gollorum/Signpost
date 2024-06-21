package gollorum.signpost.block;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class WaystoneBlockImpl extends WaystoneBlock {

    public static WaystoneBlock createInstance() {
        assert instance == null;
        return instance = new WaystoneBlockImpl();
    }
    public WaystoneBlockImpl() {
        super();
    }

    public WaystoneBlockImpl(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return WaystoneBlock.fillClonedItemStack(super.getCloneItemStack(state, target, level, pos, player), level, pos, player);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(WaystoneBlockImpl::new);
    }
}
