package gollorum.signpost.minecraft.block;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.security.WithCountRestriction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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

public class ModelWaystone extends Block implements IWaterLoggable, WithCountRestriction {

	public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;
	public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
	private static final String REGISTRY_NAME = "waystone_model";

	public static class Variant {
		public final String name;
		public final String registryName;
		public final String langPrefix;
		public final ModelWaystone block;
		public final VoxelShape shape;
		public final float modelYOffset;

		public Variant(String name, String langPrefix, VoxelShape shape, float modelYOffset) {
			this.name = name;
			registryName = REGISTRY_NAME + "_" + name;
			this.langPrefix = langPrefix;
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
	public static Variant simple_0 = new Variant("simple_0", "0", VoxelShapes.box(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 1);
	public static Variant simple_1 = new Variant("simple_1", "0", VoxelShapes.box(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 1);
	public static Variant simple_2 = new Variant("simple_2", "0", VoxelShapes.box(0.3125f, 0, 0.3125f, 0.75f, 0.6875f, 0.6875f), 0);
	public static Variant detailed_0 = new Variant("detailed_0", "1", VoxelShapes.box(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 2);
	public static Variant detailed_1 = new Variant("detailed_1", "1", VoxelShapes.box(0.25f, 0, 0.25f, 0.75f, 0.75f, 0.75f), 0);
	public static Variant aer = new Variant("aer", "2", VoxelShapes.box(0.05f, 0, 0.05f, 0.95f, 0.6f, 0.95f), 0);
	public static Variant dwarf = new Variant("dwarf", "2", VoxelShapes.box(0.05f, 0, 0.05f, 0.95f, 0.4375f, 0.95f), 2);
	public static Variant ygnar = new Variant("ygnar", "2", VoxelShapes.box(0.125f, 0, 0.125f, 0.875f, 1f, 0.875f), 0);
	public static final Variant generationMarker = simple_0;
	static {
		variants.add(simple_0);
		variants.add(simple_1);
		variants.add(simple_2);
		variants.add(detailed_0);
		variants.add(detailed_1);
		variants.add(aer);
		variants.add(dwarf);
		variants.add(ygnar);
	}

	public final Variant variant;

	private ModelWaystone(Variant variant) {
		super(Properties.of(Material.PISTON, MaterialColor.STONE)
			.strength(1.5F, 6.0F)
			.noOcclusion()
			.isViewBlocking((x, y, z) -> false));
		this.variant = variant;
		this.registerDefaultState(this.defaultBlockState().setValue(Waterlogged, false).setValue(Facing, Direction.NORTH));
	}

	@Override
	public String getDescriptionId() {
		return WaystoneBlock.INSTANCE.getDescriptionId() + "_" + variant.langPrefix + "_" + variant.name;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
		return Collections.singletonList(new ItemStack(this.asItem()));
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		WaystoneBlock.onRightClick(world, pos, player);
		return ActionResultType.CONSUME;
	}

	@Override
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(Waterlogged).add(Facing);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return defaultBlockState().setValue(Facing, context.getHorizontalDirection());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		if(!state.hasProperty(Facing)) return state;
		Direction dir = state.getValue(Facing);
		switch (rot) {
			case CLOCKWISE_90: return state.setValue(Facing, dir.getClockWise());
			case CLOCKWISE_180: return state.setValue(Facing, dir.getClockWise().getClockWise());
			case COUNTERCLOCKWISE_90: return state.setValue(Facing, dir.getCounterClockWise());
			default: return state;
		}
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		if(!state.hasProperty(Facing)) return state;
		return state.setValue(Facing, state.getValue(Facing).getOpposite());
	}

	@Override
	public boolean hasTileEntity(BlockState state) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new WaystoneTile();
	}

	@Override
	public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		WaystoneBlock.registerOwnerAndRequestGui(world, pos, placer);
	}

	@Override
	public void fillItemCategory(
		ItemGroup group, NonNullList<ItemStack> items
	) {
		if(Config.Server.allowedWaystones.get().contains(variant.name))
			super.fillItemCategory(group, items);
	}

	@SuppressWarnings("deprecation")
	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(Waterlogged) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
		return !state.getValue(Waterlogged);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
		return variant.shape;
	}

	@Override
	public BlockRestrictions.Type getBlockRestrictionType() {
		return BlockRestrictions.Type.Waystone;
	}

}
