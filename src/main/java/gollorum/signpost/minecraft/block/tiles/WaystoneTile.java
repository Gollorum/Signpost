package gollorum.signpost.minecraft.block.tiles;

import com.mojang.datafixers.types.Type;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.events.WaystoneUpdatedEvent;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.*;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class WaystoneTile extends BlockEntity implements WithOwner.OfWaystone, WaystoneContainer {

    public static final String REGISTRY_NAME = "waystone";

    private static BlockEntityType<WaystoneTile> type = null;
    public static BlockEntityType<WaystoneTile> createType() {
        assert type == null;
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, REGISTRY_NAME);
        return WaystoneTile.type = BlockEntityType.Builder.of(WaystoneTile::new, WaystoneBlock.getInstance()).build(type);
    }
    public static BlockEntityType<WaystoneTile> getBlockEntityType() {
        assert type != null;
        return type;
    }

    private Optional<PlayerHandle> owner = Optional.empty();

    private Optional<WaystoneHandle.Vanilla> handle = Optional.empty();
    public Optional<WaystoneHandle.Vanilla> getHandle() { return handle; }

    private Optional<String> name = Optional.empty();
    public Optional<String> getName() { return name; }

    public WaystoneTile(BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private final EventDispatcher.Listener<WaystoneUpdatedEvent> updateListener = event -> {
        if(WorldLocation.from(this).map(loc -> loc.equals(event.location.block)).orElse(false)) {
            name = Optional.of(event.name);
            handle = Optional.of(event.handle);
        }
        return false;
    };

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        Delay.forFrames(10, level.isClientSide(), () -> {
            WaystoneLibrary.getInstance().requestWaystoneAt(new WorldLocation(getBlockPos(), level),
                data -> {
                    handle = data.map(d -> d.handle);
                    name = data.map(d -> d.name);
                },
                level.isClientSide());
            WaystoneLibrary.getInstance().updateEventDispatcher.addListener(updateListener);
        });
    }

    public static void onRemoved(ServerLevel world, BlockPos pos) {
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
