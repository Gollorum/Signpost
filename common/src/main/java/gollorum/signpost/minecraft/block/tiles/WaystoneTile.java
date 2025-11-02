package gollorum.signpost.minecraft.block.tiles;

import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Codec;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.events.WaystoneUpdatedEvent;
import gollorum.signpost.minecraft.data.WaystoneHandleData;
import gollorum.signpost.platform.Services;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.*;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Optional;

public class WaystoneTile extends BlockEntity implements WithOwner.OfWaystone, WaystoneContainer {

    public static final String REGISTRY_NAME = "waystone";

    private static BlockEntityType<WaystoneTile> type = null;
    public static BlockEntityType<WaystoneTile> createType() {
        assert type == null;
        Type<?> type = Util.fetchChoiceType(References.BLOCK_ENTITY, REGISTRY_NAME);
        var blocks = new ArrayList<Block>();
        blocks.add(WaystoneBlock.getInstance());
        for(var variant : ModelWaystone.variants) {
            blocks.add(variant.getBlock());
        }
        return WaystoneTile.type = Services.BLOCK_ENTITY_TYPE_FACTORY.create(WaystoneTile::new, blocks.toArray(Block[]::new), type);
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
        super(getBlockEntityType(), pos, state);
    }

    private final EventDispatcher.Listener<WaystoneUpdatedEvent> updateListener = event -> {
        if (WorldLocation.from(this).map(loc -> loc.equals(event.location.block())).orElse(false)) {
            name = Optional.of(event.name);
            handle = Optional.of(event.handle);
        }
        return false;
    };

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        IDelay.forFrames(10, level.isClientSide(), () -> {
            WaystoneLibrary.getInstance().requestWaystoneAt(WorldLocation.from(getBlockPos(), level),
                data -> {
                    handle = data.map(WaystoneData::handle);
                    name = data.map(WaystoneData::name);
                },
                level.isClientSide());
            WaystoneLibrary.getInstance().updateEventDispatcher.addListener(updateListener);
        });
    }

    public static void onRemoved(ServerLevel world, BlockPos pos) {
        WaystoneLibrary.getInstance().removeAt(WorldLocation.from(pos, world), PlayerHandle.Invalid);
    }

    @Override
    public Optional<PlayerHandle> getWaystoneOwner() {
        return owner;
    }

    public void setWaystoneOwner(Optional<PlayerHandle> owner) {
        this.owner = owner;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.store(PlayerHandle.DIRECT_CODEC.optionalFieldOf("Owner"), owner);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        owner = input.read(Codec.optionalField("Owner", PlayerHandle.DIRECT_CODEC, true)).flatMap(it -> it);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        handle.ifPresent(h -> components.set(WaystoneHandleData.TYPE, new WaystoneHandleData(h)));
        name.ifPresent(n -> components.set(DataComponents.CUSTOM_NAME, Component.literal(n)));
    }
}
