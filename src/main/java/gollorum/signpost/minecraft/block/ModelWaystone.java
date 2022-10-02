package gollorum.signpost.minecraft.block;

import gollorum.signpost.BlockRestrictions;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.security.WithCountRestriction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ModelWaystone extends BaseEntityBlock implements SimpleWaterloggedBlock, WithCountRestriction {

	public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;
	public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
	private static final String REGISTRY_NAME = "waystone_model";

	public static class Variant {
		public final String name;
		public final String registryName;
		public final String langPrefix;
		private ModelWaystone block = null;
		public final VoxelShape shape;
		public final float modelYOffset;

		public Variant(String name, String langPrefix, VoxelShape shape, float modelYOffset) {
			this.name = name;
			registryName = REGISTRY_NAME + "_" + name;
			this.langPrefix = langPrefix;
			this.shape = shape;
			this.modelYOffset = modelYOffset;
		}

		public ModelWaystone createBlock() {
			assert block == null;
			return block = new ModelWaystone(this);
		}

		public ModelWaystone getBlock() {
			assert block != null;
			return block;
		}

		@Override
		public boolean equals(Object o) { return this == o || o instanceof Variant && name.equals(((Variant)o).name); }
		@Override
		public int hashCode() { return name.hashCode(); }
	}
	public static final List<Variant> variants = new ArrayList<>();
	public static Variant simple_0 = new Variant("simple_0", "0", Shapes.box(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 1);
	public static Variant simple_1 = new Variant("simple_1", "0", Shapes.box(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 1);
	public static Variant simple_2 = new Variant("simple_2", "0", Shapes.box(0.3125f, 0, 0.3125f, 0.6875f, 0.75f, 0.6875f), 0);
	public static Variant detailed_0 = new Variant("detailed_0", "1", Shapes.box(0.25f, 0, 0.25f, 0.75f, 0.5f, 0.75f), 2);
	public static Variant detailed_1 = new Variant("detailed_1", "1", Shapes.box(0.25f, 0, 0.25f, 0.75f, 0.75f, 0.75f), 0);
	public static Variant aer = new Variant("aer", "2", Shapes.box(0.05f, 0, 0.05f, 0.95f, 0.6f, 0.95f), 0);
	public static Variant dwarf = new Variant("dwarf", "2", Shapes.box(0.05f, 0, 0.05f, 0.95f, 0.4375f, 0.95f), 2);
	public static Variant ygnar = new Variant("ygnar", "2", Shapes.box(0.125f, 0, 0.125f, 0.875f, 1f, 0.875f), 0);
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
			.isViewBlocking((x, y, z) -> false)
			.requiresCorrectToolForDrops()
		);
		this.variant = variant;
		this.registerDefaultState(this.defaultBlockState().setValue(Waterlogged, false).setValue(Facing, Direction.NORTH));
	}

	@Override
	public String getDescriptionId() {
		return WaystoneBlock.getInstance().getDescriptionId() + "_" + variant.langPrefix + "_" + variant.name;
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		WaystoneBlock.onRightClick(world, pos, player);
		return InteractionResult.CONSUME;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(Waterlogged).add(Facing);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(Facing, context.getHorizontalDirection());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		if(!state.hasProperty(Facing)) return state;
		Direction dir = state.getValue(Facing);
		return switch (rot) {
			case CLOCKWISE_90 -> state.setValue(Facing, dir.getClockWise());
			case CLOCKWISE_180 -> state.setValue(Facing, dir.getClockWise().getClockWise());
			case COUNTERCLOCKWISE_90 -> state.setValue(Facing, dir.getCounterClockWise());
			default -> state;
		};
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		if(!state.hasProperty(Facing)) return state;
		return state.setValue(Facing, state.getValue(Facing).getOpposite());
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WaystoneTile(pos, state);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		WaystoneBlock.registerOwnerAndRequestGui(world, pos, placer, stack);
	}

	@Override
	public void fillItemCategory(
		CreativeModeTab group, NonNullList<ItemStack> items
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
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return !state.getValue(Waterlogged);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return variant.shape;
	}

	@Override
	public BlockRestrictions.Type getBlockRestrictionType() {
		return BlockRestrictions.Type.Waystone;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public void destroy(LevelAccessor world, BlockPos pos, BlockState state) {
		super.destroy(world, pos, state);
		if(!world.isClientSide() && world instanceof Level) {
			WaystoneTile.onRemoved((ServerLevel) world, pos);
		}
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
		return WaystoneBlock.fillClonedItemStack(super.getCloneItemStack(state, target, level, pos, player), level, pos, player);
	}


}
