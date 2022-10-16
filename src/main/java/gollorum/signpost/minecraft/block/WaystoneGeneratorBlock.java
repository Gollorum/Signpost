package gollorum.signpost.minecraft.block;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.tiles.WaystoneGeneratorEntity;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.registry.BlockRegistry;
import gollorum.signpost.minecraft.worldgen.VillageGenUtils;
import gollorum.signpost.minecraft.worldgen.VillageWaystone;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.worldgen.VillageNamesProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class WaystoneGeneratorBlock extends BaseEntityBlock {

    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final String REGISTRY_NAME = "waystone_generator";

    public WaystoneGeneratorBlock() {
        super(Properties.of(Material.WOOD, MaterialColor.WOOD));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WaystoneGeneratorEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Facing);
    }

    @javax.annotation.Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(Facing, context.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(level instanceof ServerLevel l) generate(state, pos, l, true);
        return InteractionResult.CONSUME;
    }

    public static void generate(BlockState state, BlockPos pos, ServerLevel level, boolean manuallyPlaced) {
        if(!level.getBlockState(pos).is(BlockRegistry.WaystoneGenerator.get())) return;
        if(!tryPlace(state, pos, level, manuallyPlaced))
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
    }

    private static boolean tryPlace(BlockState state, BlockPos pos, ServerLevel serverLevel, boolean manuallyPlaced) {
        BlockPos villageLocation = VillageGenUtils.getVillageLocationFor(serverLevel, pos, manuallyPlaced ? 0 : 512);
        List<ModelWaystone> allowedWaystones = getAllowedWaystones();
        if(allowedWaystones.size() == 0) {
            Signpost.LOGGER.warn("Tried to generate a waystone, but the list of allowed waystones was empty.");
            return false;
        }
        Direction facing = state.getValue(Facing);
        ModelWaystone waystone = getWaystoneType(new Random(serverLevel.getSeed() ^ pos.asLong()), allowedWaystones);
        Optional<String> optionalName = VillageNamesProvider.requestFor(pos, villageLocation, serverLevel, new Random(serverLevel.getSeed() ^ villageLocation.asLong()));
        if(optionalName.isEmpty()) {
            Signpost.LOGGER.warn("No name could be generated for waystone at " + pos + ".");
            return false;
        }
        String name = optionalName.get();
        boolean isWater = serverLevel.isWaterAt(pos.above());
        serverLevel.setBlock(
            pos,
            waystone.defaultBlockState()
                .setValue(WaystoneBlock.FACING, facing)
                .setValue(ModelWaystone.Waterlogged, isWater),
            18
        );
        WaystoneLibrary.getInstance().update(
            name,
            locationDataFor(pos, serverLevel, facing),
            null,
            false
        );
        VillageWaystone.register(name, villageLocation, serverLevel, pos);
        return true;
    }

    private static List<ModelWaystone> getAllowedWaystones() {
		return ModelWaystone.variants.stream()
			.filter(v -> Config.Server.worldGen.allowedVillageWaystones().contains(v.name))
			.map(ModelWaystone.Variant::getBlock)
			.collect(Collectors.toList());
	}

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if(!state.hasProperty(Facing)) return state;
        return state.setValue(Facing, rot.rotate(state.getValue(Facing)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        if(!state.hasProperty(Facing)) return state;
        return state.setValue(Facing, state.getValue(Facing).getOpposite());
    }

    private static WaystoneLocationData locationDataFor(BlockPos pos, ServerLevel world, Direction facing) {
		return new WaystoneLocationData(new WorldLocation(pos, world.getLevel()), spawnPosFor(world, pos, facing));
	}

    private static Vector3 spawnPosFor(ServerLevel world, BlockPos waystonePos, Direction facing) {
		BlockPos spawnBlockPos = waystonePos.relative(facing, -2);
		int maxOffset = 10;
		int offset = 0;
        while(isFree(world.getBlockState(spawnBlockPos), world, spawnBlockPos) && offset <= maxOffset) {
			spawnBlockPos = spawnBlockPos.below();
			offset++;
		}
		offset = 0;
        while(!isFree(world.getBlockState(spawnBlockPos), world, spawnBlockPos) && offset <= maxOffset) {
			spawnBlockPos = spawnBlockPos.above();
			offset++;
		}
		return Vector3.fromBlockPos(spawnBlockPos).add(0.5f, 0, 0.5f);
	}

    private static boolean isFree(BlockState state, ServerLevel world, BlockPos waystonePos) {
        return state.getCollisionShape(world, waystonePos).isEmpty();
    }

    private static ModelWaystone getWaystoneType(Random random, List<ModelWaystone> allowedWaystones) {
		return allowedWaystones.get(random.nextInt(allowedWaystones.size()));
	}

}
