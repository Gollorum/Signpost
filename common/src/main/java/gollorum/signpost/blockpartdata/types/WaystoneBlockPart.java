package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.events.WaystoneUpdatedEvent;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.*;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.OptionalSerializerV1;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class WaystoneBlockPart implements BlockPart<WaystoneBlockPart>, WithOwner.OfWaystone {

	private Optional<PlayerHandle> owner;
    private Optional<WaystoneHandle.Vanilla> handle = Optional.empty();
    private Optional<String> name = Optional.empty();

    public Optional<WaystoneHandle.Vanilla> getHandle() { return handle; }
    public Optional<String> getName() { return name; }

	private static final AABB BOUNDS = new AABB(
		new Vector3(-3, -8, -3),
		new Vector3(3, -2, 3)
	).map(CoordinatesUtil::voxelToLocal);

	public static final BlockPartMetadata<WaystoneBlockPart> METADATA = new BlockPartMetadata<>(
		"Waystone",
        version -> (version < 2
            ? OptionalSerializerV1.of(PlayerHandle.WRAPPED_CODEC).fieldOf("owner")
            : PlayerHandle.DIRECT_CODEC.optionalFieldOf("owner")
        ).xmap(WaystoneBlockPart::new, w -> w.owner),
		ByteBufCodecs.optional(PlayerHandle.STREAM_CODEC)
			.map(WaystoneBlockPart::new, w -> w.owner)
			.mapStream(it -> it),
        WaystoneBlockPart.class
	);

	public WaystoneBlockPart(Optional<PlayerHandle> owner) { this.owner = owner; }
	public WaystoneBlockPart(PlayerHandle owner) { this.owner = Optional.of(owner); }

    private EventDispatcher.Listener<WaystoneUpdatedEvent> updateListener;

    public void initialize(Level level, BlockPos position) {
        if (updateListener != null) return; // already initialized

        var location = WorldLocation.from(position, level);
        updateListener = event -> {
            if (location.equals(event.location.block())) {
                name = Optional.of(event.name);
                handle = Optional.of(event.handle);
            }
            return false;
        };
        IDelay.forFrames(10, level.isClientSide(), () -> {
            WaystoneLibrary.getInstance().requestWaystoneAt(location,
                data -> {
                    handle = data.map(WaystoneData::handle);
                    name = data.map(WaystoneData::name);
                },
                level.isClientSide());
            WaystoneLibrary.getInstance().updateEventDispatcher.addListener(updateListener);
        });
    }

    @Override
    public void attachTo(PostTile tile) {
        initialize(tile.getLevel(), tile.getBlockPos());
    }

    @Override
	public Intersectable<Ray, Float> getIntersection() { return BOUNDS; }

	@Override
	public InteractionResult interact(InteractionInfo info) {
		WaystoneBlock.onRightClick(info.player.level(), info.tile.getBlockPos(), info.player);
		return InteractionResult.Accepted;
	}

	@Override
	public BlockPartMetadata<WaystoneBlockPart> getMeta() { return METADATA; }

    @Override
	public boolean hasThePermissionToEdit(WithOwner tile, Player player) { return true; }

	@Override
	public Collection<ItemStack> getDrops() {
		return Collections.singleton(new ItemStack(WaystoneBlock.getInstance().asItem()));
	}

	@Override
	public void removeFrom(PostTile tile) {
		if(tile.hasLevel() && !tile.getLevel().isClientSide()) {
			Optional<WorldLocation> location = WorldLocation.from(tile);
			if(location.isPresent())
				WaystoneLibrary.getInstance().removeAt(location.get(), PlayerHandle.Invalid);
			else
                Signpost.LOGGER.error("Waystone tile at {} was removed but world was null. This means that the waystone has not been cleaned up correctly.", tile.getBlockPos());
		}
        WaystoneLibrary.getInstance().updateEventDispatcher.removeListener(updateListener);
	}

	@Override
	public Optional<PlayerHandle> getWaystoneOwner() {
		return owner;
	}

    @Override
    public void setWaystoneOwner(Optional<PlayerHandle> owner) {
        this.owner = owner;
    }

    @Override
	public Collection<Texture> getAllTextures() {
		return Collections.singleton(TextureResource.waystoneTextureLocation);
	}

}
