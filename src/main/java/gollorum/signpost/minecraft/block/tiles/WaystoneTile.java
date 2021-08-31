package gollorum.signpost.minecraft.block.tiles;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.WaystoneContainer;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class WaystoneTile extends BlockEntity implements WithOwner.OfWaystone, WaystoneContainer {

    public static final String REGISTRY_NAME = "waystone";

    public static final BlockEntityType<WaystoneTile> type = BlockEntityType.Builder.of(WaystoneTile::new, WaystoneBlock.INSTANCE).build(null);

    private Optional<PlayerHandle> owner = Optional.empty();

    public WaystoneTile(BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if(Signpost.getServerType().isServer) {
            Optional<WorldLocation> location = WorldLocation.from(this);
            if(location.isPresent())
                WaystoneLibrary.getInstance().removeAt(location.get(), PlayerHandle.Invalid);
            else Signpost.LOGGER.error("Waystone tile at "+ getBlockPos() +"  was removed but world was null. " +
                "This means that the waystone has not been cleaned up correctly.");
        }
    }

    @Override
    public Optional<PlayerHandle> getWaystoneOwner() {
        return owner;
    }

    public void setWaystoneOwner(Optional<PlayerHandle> owner) {
        this.owner = owner;
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        compound.put("Owner", PlayerHandle.Serializer.optional().write(owner));
        return super.save(compound);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        owner = PlayerHandle.Serializer.optional().read(compound.getCompound("Owner"));
    }
}
