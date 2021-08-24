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

    public static final TileEntityType<WaystoneTile> type = TileEntityType.Builder.create(WaystoneTile::new, WaystoneBlock.INSTANCE).build(null);

    private Optional<PlayerHandle> owner = Optional.empty();

    public WaystoneTile() {
        super(type);
    }

    @Override
    public void remove() {
        super.remove();
        if(Signpost.getServerType().isServer) {
            Optional<WorldLocation> location = WorldLocation.from(this);
            if(location.isPresent())
                WaystoneLibrary.getInstance().removeAt(location.get(), PlayerHandle.Invalid);
            else Signpost.LOGGER.error("Waystone tile at "+ pos +"  was removed but world was null. " +
                "This means that the waystone has not been cleaned up correctly.");
        }
    }

    @Override
    public void setWorldAndPos(World world, BlockPos pos) {
        if(!world.isRemote) {
            Optional<WorldLocation> oldLocation = WorldLocation.from(this);
            oldLocation.ifPresent(worldLocation -> WaystoneLibrary.getInstance().updateLocation(
                worldLocation,
                new WorldLocation(pos, world)
            ));
        }
        super.setWorldAndPos(world, pos);
    }

    @Override
    public void setPos(BlockPos pos) {
        Optional<WorldLocation> oldLocation = WorldLocation.from(this);
        super.setPos(pos);
        Optional<WorldLocation> newLocation = WorldLocation.from(this);
        if(oldLocation.isPresent() && newLocation.isPresent() && !world.isRemote)
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
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("Owner", PlayerHandle.Serializer.optional().write(owner));
        return super.write(compound);
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        super.read(state, compound);
        owner = PlayerHandle.Serializer.optional().read(compound.getCompound("Owner"));
    }
}
