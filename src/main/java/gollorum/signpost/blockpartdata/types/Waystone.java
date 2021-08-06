package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Intersectable;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class Waystone implements BlockPart<Waystone>, WithOwner.OfWaystone {

	private Optional<PlayerHandle> owner;

	private static final AABB BOUNDS = new AABB(
		new Vector3(-3, -8, -3),
		new Vector3(3, -2, 3)
	).map(CoordinatesUtil::voxelToLocal);

	public static final BlockPartMetadata<Waystone> METADATA = new BlockPartMetadata<>(
		"Waystone",
		Waystone::writeTo,
		(compound) -> new Waystone(PlayerHandle.Serializer.optional().read(compound.getCompound("owner"))),
        Waystone.class
	);

	public Waystone(Optional<PlayerHandle> owner) { this.owner = owner; }
	public Waystone(PlayerHandle owner) { this.owner = Optional.of(owner); }

	@Override
	public Intersectable<Ray, Float> getIntersection() { return BOUNDS; }

	@Override
	public InteractionResult interact(InteractionInfo info) {
		gollorum.signpost.minecraft.block.Waystone.onRightClick(info.player.world, info.tile.getPos(), info.player);
		return InteractionResult.Accepted;
	}

	@Override
	public BlockPartMetadata<Waystone> getMeta() { return METADATA; }

	@Override
	public void writeTo(CompoundNBT compound) {
		compound.put("owner", PlayerHandle.Serializer.optional().write(owner, new CompoundNBT()));
	}

	@Override
	public void readMutationUpdate(CompoundNBT compound, TileEntity tile, PlayerEntity editingPlayer) {
		if(compound.contains("owner"))
			owner = PlayerHandle.Serializer.optional().read(compound.getCompound("owner"));
	}

	@Override
	public boolean hasThePermissionToEdit(WithOwner tile, @Nullable PlayerEntity player) { return true; }

	@Override
	public Collection<ItemStack> getDrops(PostTile tile) {
		return Collections.singleton(new ItemStack(gollorum.signpost.minecraft.block.Waystone.INSTANCE.asItem()));
	}

	@Override
	public void removeFrom(PostTile tile) {
		if(tile.hasWorld() && !tile.getWorld().isRemote) {
			Optional<WorldLocation> location = WorldLocation.from(tile);
			if(location.isPresent())
				WaystoneLibrary.getInstance().removeAt(location.get(), PlayerHandle.Invalid);
			else Signpost.LOGGER.error("Waystone tile at "+ tile.getPos() +"  was removed but world was null. " +
				"This means that the waystone has not been cleaned up correctly.");
		}
		getWaystoneOwner().ifPresent(o -> BlockRestrictions.getInstance().incrementRemaining(BlockRestrictions.Type.Waystone, o));
	}

	@Override
	public Optional<PlayerHandle> getWaystoneOwner() {
		return owner;
	}
}
