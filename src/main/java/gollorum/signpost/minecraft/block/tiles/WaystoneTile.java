package gollorum.signpost.minecraft.block.tiles;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.WaystoneContainer;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class WaystoneTile extends BlockEntity implements WithOwner.OfWaystone, WaystoneContainer {

    public static final String REGISTRY_NAME = "waystone";

    private static BlockEntityType<WaystoneTile> type = null;
    public static BlockEntityType<WaystoneTile> createType() {
        return type = BlockEntityType.Builder.of(WaystoneTile::new, WaystoneBlock.getInstance()).build(null);
    }

    private Optional<PlayerHandle> owner = Optional.empty();

    public WaystoneTile(BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void onRemoved(ServerLevel world, BlockPos pos) {
        if(!world.isClientSide())
            WaystoneLibrary.getInstance().removeAt(new WorldLocation(pos, world), PlayerHandle.Invalid);
    }

    @Override
    public Optional<PlayerHandle> getWaystoneOwner() {
        return owner;
    }

    public void setWaystoneOwner(Optional<PlayerHandle> owner) {
        this.owner = owner;
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        compound.put("Owner", PlayerHandle.Serializer.optional().write(owner));
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        owner = PlayerHandle.Serializer.optional().read(compound.getCompound("Owner"));
    }
}
