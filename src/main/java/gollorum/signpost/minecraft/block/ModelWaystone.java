package gollorum.signpost.minecraft.block;

import gollorum.signpost.minecraft.Config;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModelWaystone extends Block implements IWaterLoggable  {

	public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;
	public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
	private static final String REGISTRY_NAME = "waystone_model";

	public static class Variant {
		public final String name;
		public final String registryName;
		public final ModelWaystone block;
		public final VoxelShape shape;
		public final float modelYOffset;

		public Variant(String name, VoxelShape shape, float modelYOffset) {
			this.name = name;
			registryName = REGISTRY_NAME + "_" + name;
			this.shape = shape;
			this.modelYOffset = modelYOffset;
			block = new ModelWaystone(this);
		}
		@Override
		public boolean equals(Object o) { return this == o || o instanceof Variant && name.equals(((Variant)o).name); }
		@Override
		public int hashCode() { return name.hashCode(); }
	}
	public static final List<Variant> variants = new ArrayList<>();
	public static final Variant generationMarker;
	static {
		variants.add(generationMarker = new Variant("simple0", VoxelShapes.create(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 1));
		variants.add(new Variant("simple1", VoxelShapes.create(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 1));
		variants.add(new Variant("simple2", VoxelShapes.create(0.3125f, 0, 0.3125f, 0.75f, 0.6875f, 0.6875f), 0));
		variants.add(new Variant("detailed0", VoxelShapes.create(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 2));
		variants.add(new Variant("detailed1", VoxelShapes.create(0.25f, 0, 0.25f, 0.75f, 0.75f, 0.75f), 0));
		variants.add(new Variant("aer", VoxelShapes.create(0.05f, 0, 0.05f, 0.95f, 0.6f, 0.95f), 0));
		variants.add(new Variant("dwarf", VoxelShapes.create(0.05f, 0, 0.05f, 0.95f, 0.4375f, 0.95f), 2));
		variants.add(new Variant("ygnar", VoxelShapes.create(0.125f, 0, 0.125f, 0.875f, 1f, 0.875f), 0));
	}

	public final Variant variant;

	private ModelWaystone(Variant variant) {
		super(Properties.create(Material.PISTON, MaterialColor.STONE)
			.hardnessAndResistance(1.5F, 6.0F)
			.notSolid()
			.setOpaque((x, y, z) -> false));
		this.variant = variant;
		this.setDefaultState(this.getDefaultState().with(Waterlogged, false).with(Facing, Direction.NORTH));
	}

	@Override
	public String getTranslationKey() {
		return Waystone.INSTANCE.getTranslationKey();
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
		return Collections.singletonList(new ItemStack(this.asItem()));
	}

	@Override
	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		Waystone.onRightClick(world, pos, player);
		return ActionResultType.CONSUME;
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(Waterlogged).add(Facing);
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
	public boolean hasTileEntity(BlockState state) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new WaystoneTile();
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity player, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, player, stack);
		if(!world.isRemote && player instanceof ServerPlayerEntity)
			Waystone.openGuiIfHasPermission((ServerPlayerEntity) player, new WorldLocation(pos, world));
	}

	@Override
	public void fillItemGroup(
		ItemGroup group, NonNullList<ItemStack> items
	) {
		if(Config.Server.allowedWaystones.get().contains(variant.name))
			super.fillItemGroup(group, items);
	}

	@SuppressWarnings("deprecation")
	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(Waterlogged) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
		return !state.get(Waterlogged);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
		return variant.shape;
	}

}
