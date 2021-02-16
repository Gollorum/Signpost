package gollorum.signpost.minecraft.block;

import com.google.common.collect.ImmutableList;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.Sign;
import gollorum.signpost.blockpartdata.types.SmallShortSign;
import gollorum.signpost.blockpartdata.types.SmallWideSign;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.Colors;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.worldgen.WorldGenUtils;
import javafx.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class VillagePost  extends Block {

	private static final Map<BlockPos, Collection<WaystoneHandle>> generatedTargets = new HashMap<>();

	public static void reset() { generatedTargets.clear(); }

	public static final String REGISTRY_NAME = "village_signpost";
	public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;

	public static final VillagePost INSTANCE = new VillagePost();

	private static final float smallSignRatio = 0.5f;

	private VillagePost() {
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
		if(!(worldIn instanceof ServerWorld && worldIn.getBlockState(pos).getBlock() instanceof VillagePost)) return Blocks.AIR.getDefaultState();
		ServerWorld world = (ServerWorld) worldIn;
		BlockPos referencePos = WorldGenUtils.findNearestVillage(world, pos, 100).orElse(pos);
		Post.Variant variant = generate(state, world, pos, referencePos);
		if(variant != null) {
			world.setBlockState(pos.up(), variant.block.getDefaultState());
			Delay.onServerForFrames(2, () -> {
				world.setBlockState(pos, variant.block.getDefaultState());
				Delay.onServerUntil(
					() -> world.getTileEntity(pos) instanceof PostTile,
					() -> {
						PostTile tile = (PostTile) world.getTileEntity(pos);
						tile.addPart(new BlockPartInstance(
							new gollorum.signpost.blockpartdata.types.Post(variant.type.postTexture),
							Vector3.ZERO), ItemStack.EMPTY, PlayerHandle.Invalid);
					}, 20);
			});
		}
		return Blocks.AIR.getDefaultState();
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if(!(worldIn instanceof ServerWorld && worldIn.getBlockState(pos).getBlock() instanceof VillagePost)) return;
		ServerWorld world = (ServerWorld) worldIn;
		Post.Variant variant = generate(state, world, pos, pos);
		Delay.onServerForFrames(1, () -> {
			if(variant != null) {
				world.setBlockState(pos, variant.block.getDefaultState());
				world.setBlockState(pos.up(), variant.block.getDefaultState());
				Delay.onServerUntil(
					() -> world.getTileEntity(pos) instanceof PostTile,
					() -> world.getTileEntity(pos).markDirty(),
					20
				);
			} else {
				world.setBlockState(pos, Blocks.AIR.getDefaultState());
			}
		});
	}

	private static Post.Variant generate(BlockState state, ServerWorld world, BlockPos lowerPos, BlockPos referencePos) {
		Biome.Category biome = world.getBiome(referencePos).getCategory();
		Post.Variant variant = Post.forBiome(biome);
		Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets = VillageWaystone.getAllEntries().stream()
			.filter(e -> !e.getKey().equals(referencePos))
			.map(e -> new Pair<>(e, (float) Math.sqrt(e.getKey().distanceSq(referencePos)) * (0.5f + world.rand.nextFloat())))
			.sorted((e1, e2) -> Float.compare(e1.getValue(), e2.getValue()))
			.map(Pair::getKey)
			.filter(e -> WaystoneLibrary.getInstance().contains(e.getValue()) &&
				!generatedTargets.computeIfAbsent(referencePos, x -> new HashSet<>())
					.contains(e.getValue()))
			.collect(Collectors.toCollection(LinkedList::new));
		if(possibleTargets.isEmpty()) return null;
		else {
			Direction facing = state.hasProperty(Facing) ? state.get(Facing) : Direction.SOUTH;
			BlockPos upperPos = lowerPos.up();
			Delay.onServerUntil(() -> world.getTileEntity(upperPos) instanceof PostTile, () -> {
				PostTile tile = (PostTile) world.getTileEntity(upperPos);
				makeSign(world, facing, tile, variant, possibleTargets, 0.75f, biome);
				makeSign(world, facing, tile, variant, possibleTargets, 0.25f, biome);
				tile.addPart(new BlockPartInstance(
					new gollorum.signpost.blockpartdata.types.Post(variant.type.postTexture),
					Vector3.ZERO
				), ItemStack.EMPTY, PlayerHandle.Invalid);
				tile.markDirty();
//				tile.requestModelDataUpdate();
			}, 20);
			return variant;
		}
	}

	private static Collection<WaystoneHandle> makeSign(ServerWorld world, Direction facing, PostTile tile, Post.Variant variant, Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets, float y, Biome.Category biome) {
		if(possibleTargets.isEmpty()) return Collections.emptySet();
		return world.rand.nextFloat() < smallSignRatio
			? makeShortSigns(facing, tile, variant, possibleTargets, y, biome)
			: makeWideSign(facing, tile, variant, possibleTargets, y, biome);
	}

	private static Collection<WaystoneHandle> makeWideSign(Direction facing, PostTile tile, Post.Variant variant, Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets, float y, Biome.Category biome) {
		Map.Entry<BlockPos, WaystoneHandle> target = possibleTargets.poll();
		if(target == null) return Collections.emptySet();
		WaystoneData targetData = WaystoneLibrary.getInstance().getData(target.getValue());
		Angle rotation = Sign.pointingAt(tile.getPos(), target.getKey());
		tile.addPart(
			new BlockPartInstance(
				new SmallWideSign(
					rotation, targetData.name, shouldFlip(facing, rotation),
					variant.type.mainTexture, variant.type.secondaryTexture,
					overlayFor(biome), Colors.black, Optional.of(target.getValue()),
					ItemStack.EMPTY, variant.type
				),
				new Vector3(0, y, 0)
			),
			ItemStack.EMPTY,
			PlayerHandle.Invalid
		);
		return Collections.singleton(target.getValue());
	}

	private static Collection<WaystoneHandle> makeShortSigns(Direction facing, PostTile tile, Post.Variant variant, Queue<Map.Entry<BlockPos, WaystoneHandle>> possibleTargets, float y, Biome.Category biome) {
		Map.Entry<BlockPos, WaystoneHandle> target = possibleTargets.poll();
		if(target == null) Collections.emptySet();
		WaystoneData targetData = WaystoneLibrary.getInstance().getData(target.getValue());
		Angle rotation = Sign.pointingAt(tile.getPos(), target.getKey());
		boolean shouldFlip = shouldFlip(facing, rotation);
		Optional<Overlay> overlay = overlayFor(biome);
		tile.addPart(
			new BlockPartInstance(
				new SmallShortSign(
					rotation, targetData.name, shouldFlip,
					variant.type.mainTexture, variant.type.secondaryTexture,
					overlay, Colors.black, Optional.of(target.getValue()),
					ItemStack.EMPTY, variant.type
				),
				new Vector3(0, y, 0)
			),
			ItemStack.EMPTY,
			PlayerHandle.Invalid
		);
		Map.Entry<BlockPos, WaystoneHandle> secondTarget = possibleTargets.poll();
		while(secondTarget != null) {
			WaystoneData secondTargetData = WaystoneLibrary.getInstance().getData(secondTarget.getValue());
			Angle secondRotation = Sign.pointingAt(tile.getPos(), secondTarget.getKey());
			boolean shouldSecondFlip = shouldFlip(facing, secondRotation);
			if(shouldSecondFlip ^ shouldFlip) {
				secondTarget = possibleTargets.poll();
				continue;
			}
			tile.addPart(
				new BlockPartInstance(
					new SmallShortSign(
						secondRotation, secondTargetData.name, shouldSecondFlip,
						variant.type.mainTexture, variant.type.secondaryTexture,
						overlay, Colors.black, Optional.of(secondTarget.getValue()),
						ItemStack.EMPTY, variant.type
					),
					new Vector3(0, y, 0)
				),
				ItemStack.EMPTY,
				PlayerHandle.Invalid
			);
			break;
		}
		return secondTarget == null
			? Collections.singleton(target.getValue())
			: ImmutableList.of(target.getValue(), secondTarget.getValue());
	}

	private static boolean shouldFlip(Direction facing, Angle signRotation) {
		float degrees = signRotation.subtract(Angle.fromDegrees(facing.getHorizontalAngle())).normalized().degrees();
		return degrees < 0;
	}

	private static Optional<Overlay> overlayFor(Biome.Category biome) {
		switch (biome) {
			case ICY: return Optional.of(Overlay.Snow);
			case JUNGLE: return Optional.of(Overlay.Vine);
			default: return Optional.empty();
		}
	}

}
