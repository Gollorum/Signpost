package gollorum.signpost.blockpartdata.types;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.interactions.InteractionInfo;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class Waystone implements BlockPart<Waystone> {

	private static final AABB BOUNDS = new AABB(
		new Vector3(-3, -8, -3),
		new Vector3(3, -2, 3)
	).map(CoordinatesUtil::voxelToLocal);

	public static final BlockPartMetadata<Waystone> METADATA = new BlockPartMetadata<>(
		"Waystone",
		(post, compound) -> {},
		(compound) -> new Waystone()
	);

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
	public void writeTo(CompoundNBT compound) { }

	@Override
	public void readMutationUpdate(CompoundNBT compound, TileEntity tile, PlayerEntity editingPlayer) { }

	@Override
	public boolean hasThePermissionToEdit(PlayerEntity player) { return true; }

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
	}
}
