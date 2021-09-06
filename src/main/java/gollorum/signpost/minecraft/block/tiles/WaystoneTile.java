package gollorum.signpost.minecraft.block.tiles;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.WaystoneContainer;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public class WaystoneTile extends TileEntity implements WithOwner.OfWaystone, WaystoneContainer {

    public static final String REGISTRY_NAME = "waystone";

    public static final TileEntityType<WaystoneTile> type = TileEntityType.Builder.of(WaystoneTile::new, WaystoneBlock.INSTANCE).build(null);

    private Optional<PlayerHandle> owner = Optional.empty();

    public WaystoneTile() {
        super(type);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if(hasLevel() && !getLevel().isClientSide()) {
            Optional<WorldLocation> location = WorldLocation.from(this);
            if(location.isPresent())
                WaystoneLibrary.getInstance().removeAt(location.get(), PlayerHandle.Invalid);
            else Signpost.LOGGER.error("Waystone tile at "+ getBlockPos() +"  was removed but world was null. " +
                "This means that the waystone has not been cleaned up correctly.");
        }
    }

    @Override
    public void setLevelAndPosition(World world, BlockPos pos) {
        if(!world.isClientSide()) {
            Optional<WorldLocation> oldLocation = WorldLocation.from(this);
            oldLocation.ifPresent(worldLocation -> WaystoneLibrary.getInstance().updateLocation(
                worldLocation,
                new WorldLocation(pos, world)
            ));
        }
        super.setLevelAndPosition(world, pos);
    }

    @Override
    public void setPosition(BlockPos pos) {
        Optional<WorldLocation> oldLocation = WorldLocation.from(this);
        super.setPosition(pos);
        Optional<WorldLocation> newLocation = WorldLocation.from(this); // getLevel() has to be non-null if this is present.
        if(oldLocation.isPresent() && newLocation.isPresent() && !getLevel().isClientSide())
            WaystoneLibrary.getInstance().updateLocation(oldLocation.get(), newLocation.get());
    }

    @Override
    public Optional<PlayerHandle> getWaystoneOwner() {
        return owner;
    }

    public void setWaystoneOwner(Optional<PlayerHandle> owner) {
        this.owner = owner;
    }

    @Override
    public CompoundNBT save(CompoundNBT compound) {
        compound.put("Owner", PlayerHandle.Serializer.optional().write(owner));
        return super.save(compound);
    }

    @Override
    public void load(BlockState state, CompoundNBT compound) {
        super.load(state, compound);
        owner = PlayerHandle.Serializer.optional().read(compound.getCompound("Owner"));
    }
}
