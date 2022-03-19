package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class WaystoneBlockPart implements BlockPart<WaystoneBlockPart>, WithOwner.OfWaystone {

	private Optional<PlayerHandle> owner;

	private static final AABB BOUNDS = new AABB(
		new Vector3(-3, -8, -3),
		new Vector3(3, -2, 3)
	).map(CoordinatesUtil::voxelToLocal);

	public static final BlockPartMetadata<WaystoneBlockPart> METADATA = new BlockPartMetadata<>(
		"Waystone",
		WaystoneBlockPart::writeTo,
		(compound) -> new WaystoneBlockPart(PlayerHandle.Serializer.optional().read(compound.getCompound("owner"))),
        WaystoneBlockPart.class
	);

	public WaystoneBlockPart(Optional<PlayerHandle> owner) { this.owner = owner; }
	public WaystoneBlockPart(PlayerHandle owner) { this.owner = Optional.of(owner); }

	@Override
	public Intersectable<Ray, Float> getIntersection() { return BOUNDS; }

	@Override
	public InteractionResult interact(InteractionInfo info) {
		WaystoneBlock.onRightClick(info.player.level, info.tile.getBlockPos(), info.player);
		return InteractionResult.Accepted;
	}

	@Override
	public BlockPartMetadata<WaystoneBlockPart> getMeta() { return METADATA; }

	@Override
	public void writeTo(CompoundTag compound) {
		compound.put("owner", PlayerHandle.Serializer.optional().write(owner, new CompoundTag()));
	}

	@Override
	public void readMutationUpdate(CompoundTag compound, BlockEntity tile, Player editingPlayer) {
		if(compound.contains("owner"))
			owner = PlayerHandle.Serializer.optional().read(compound.getCompound("owner"));
	}

	@Override
	public boolean hasThePermissionToEdit(WithOwner tile, @Nullable Player player) { return true; }

	@Override
	public Collection<ItemStack> getDrops(PostTile tile) {
		return Collections.singleton(new ItemStack(WaystoneBlock.getInstance().asItem()));
	}

	@Override
	public void removeFrom(PostTile tile) {
		if(tile.hasLevel() && !tile.getLevel().isClientSide()) {
			Optional<WorldLocation> location = WorldLocation.from(tile);
			if(location.isPresent())
				WaystoneLibrary.getInstance().removeAt(location.get(), PlayerHandle.Invalid);
			else Signpost.LOGGER.error("Waystone tile at "+ tile.getBlockPos() +"  was removed but world was null. " +
				"This means that the waystone has not been cleaned up correctly.");
			getWaystoneOwner().ifPresent(o -> BlockRestrictions.getInstance().incrementRemaining(BlockRestrictions.Type.Waystone, o));
		}
	}

	@Override
	public Optional<PlayerHandle> getWaystoneOwner() {
		return owner;
	}

	@Override
	public Collection<ResourceLocation> getAllTextures() {
		return Collections.singleton(TextureResource.waystoneTextureLocation);
	}

}
