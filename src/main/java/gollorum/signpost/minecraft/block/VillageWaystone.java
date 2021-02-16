package gollorum.signpost.minecraft.block;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.Config;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.worldgen.VillageNamesProvider;
import gollorum.signpost.worldgen.WorldGenUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class VillageWaystone extends Block {
	public static final String REGISTRY_NAME = "village_waystone";
	public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;

	public static final VillageWaystone INSTANCE = new VillageWaystone();

	private static final Map<BlockPos, WaystoneHandle> generatedWaystones = new HashMap<>();

	public static void reset() { generatedWaystones.clear(); }

	public static INBT serialize() {
		ListNBT ret = new ListNBT();
		ret.addAll(generatedWaystones.entrySet().stream().map(e -> {
			CompoundNBT compound = new CompoundNBT();
			BlockPosSerializer.INSTANCE.writeTo(e.getKey(), compound, "refPos");
			WaystoneHandle.SERIALIZER.writeTo(e.getValue(), compound, "waystone");
			return compound;
		}).collect(Collectors.toList()));
		return ret;
	}

	public static void deserialize(ListNBT nbt) {
		generatedWaystones.clear();
		generatedWaystones.putAll(
			nbt.stream().collect(Collectors.toMap(
				entry -> BlockPosSerializer.INSTANCE.read((CompoundNBT) entry, "refPos"),
				entry -> WaystoneHandle.SERIALIZER.read((CompoundNBT) entry, "waystone")
		)));
	}

	private VillageWaystone() {
		super(Properties.create(Material.ROCK, MaterialColor.STONE)
			.hardnessAndResistance(1.5F, 6.0F).setNeedsPostProcessing((x, y, z) -> true));
		this.setDefaultState(this.getDefaultState().with(Facing, Direction.SOUTH));
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(Facing);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return getDefaultState().with(Facing, context.getPlacementHorizontalFacing());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		if(!state.hasProperty(Facing)) return state;
		Direction dir = state.get(Facing);
		switch (rot) {
			case CLOCKWISE_90: return state.with(Facing, dir.rotateY());
			case CLOCKWISE_180: return state.with(Facing, dir.rotateY().rotateY());
			case COUNTERCLOCKWISE_90: return state.with(Facing, dir.rotateYCCW());
			default: return state;
		}
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		if(!state.hasProperty(Facing)) return state;
		return state.with(Facing, state.get(Facing).getOpposite());
	}

	@Override
	public BlockState updatePostPlacement(
		BlockState state,
		Direction facing,
		BlockState facingState,
		IWorld worldIn,
		BlockPos pos,
		BlockPos facingPos
	) {
		if(!(worldIn instanceof ServerWorld)) return state;
		ServerWorld world = (ServerWorld) worldIn;
		List<ModelWaystone> allowedWaystones = getAllowedWaystones();
		if(allowedWaystones.size() == 0) {
			Signpost.LOGGER.warn("Tried to generate a waystone, but the list of allowed waystones was empty.");
			return Blocks.AIR.getDefaultState();
		}
		BlockPos referencePos = WorldGenUtils.findNearestVillage(world, pos, 100).orElse(pos);
		Random random = randomFor(world, referencePos);
		VillageNamesProvider.requestFor(pos, referencePos, world, random, name ->
			Delay.onServerForFrames(2, () -> {
				if(generatedWaystones.containsKey(referencePos)) {
					world.setBlockState(pos.down(), world.getBlockState(pos.add(1, -1, 0)));
				} else {
					Direction direction = state.hasProperty(Facing) ? state.get(Facing) : Direction.SOUTH;
					ModelWaystone waystone = getWaystoneType(random, allowedWaystones);
					world.setBlockState(pos, waystone.getDefaultState().with(ModelWaystone.Facing, direction));
					WaystoneLibrary.getInstance().update(
						name,
						new WaystoneLocationData(new WorldLocation(pos, world), spawnPosFor(world, pos, direction)),
						PlayerHandle.Invalid,
						false
					);
					WaystoneLibrary.getInstance().getHandleByName(name).ifPresent(handle -> generatedWaystones.put(referencePos, handle));
				}
			}),
		() -> Signpost.LOGGER.warn("No name could be generated for waystone at " + pos + ".")
		);
		return Blocks.AIR.getDefaultState();
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		if(!(worldIn instanceof ServerWorld)) return;
		ServerWorld world = (ServerWorld) worldIn;
		List<ModelWaystone> allowedWaystones = getAllowedWaystones();
		if(allowedWaystones.size() == 0) {
			Signpost.LOGGER.warn("Tried to generate a waystone, but the list of allowed waystones was empty.");
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
			return;
		}
		Random random = randomFor(world, pos);
		ModelWaystone waystone = getWaystoneType(random, allowedWaystones);
		Direction direction = state.hasProperty(Facing) ? state.get(Facing) : Direction.SOUTH;
		world.setBlockState(pos, waystone.getDefaultState().with(ModelWaystone.Facing, direction));
		VillageNamesProvider.requestFor(pos, pos, world, random, name -> {
			WaystoneLibrary.getInstance().update(
				name,
				new WaystoneLocationData(new WorldLocation(pos, world), spawnPosFor(world, pos, direction)),
				PlayerHandle.Invalid,
				false
			);
			WaystoneLibrary.getInstance().getHandleByName(name).ifPresent(handle -> generatedWaystones.put(pos, handle));
		}, () -> {
			Signpost.LOGGER.warn("No name could be generated for waystone at " + pos + ".");
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
		});
	}

	private static List<ModelWaystone> getAllowedWaystones() {
		return ModelWaystone.variants.stream()
			.filter(v -> Config.Server.allowedVillageWaystones.get().contains(v.name))
			.map(v -> v.block)
			.collect(Collectors.toList());
	}

	private static Random randomFor(ServerWorld world, BlockPos pos) {
		return new Random((world.getSeed() << 4) ^ pos.hashCode());
	}

	private static ModelWaystone getWaystoneType(Random random, List<ModelWaystone> allowedWaystones) {
		return allowedWaystones.get(random.nextInt(allowedWaystones.size()));
	}

	private static Vector3 spawnPosFor(World world, BlockPos waystonePos, Direction facing) {
		BlockPos spawnBlockPos = waystonePos.offset(facing.getOpposite(), 2);
		while(world.getBlockState(spawnBlockPos).isAir())
			spawnBlockPos = spawnBlockPos.down();
		while(!world.getBlockState(spawnBlockPos).isAir())
			spawnBlockPos = spawnBlockPos.up();
		return Vector3.fromBlockPos(spawnBlockPos).add(0.5f, 0, 0.5f);
	}

	public static Set<Map.Entry<BlockPos, WaystoneHandle>> getAllEntries() {
		return generatedWaystones.entrySet();
	}
}
