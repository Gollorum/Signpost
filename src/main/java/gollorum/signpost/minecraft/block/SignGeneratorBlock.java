package gollorum.signpost.minecraft.block;

import gollorum.signpost.minecraft.block.tiles.SignGeneratorEntity;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.worldgen.VillageSignpost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
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

public class SignGeneratorBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String REGISTRY_NAME = "sign_generator";

    public SignGeneratorBlock() {
        super(Properties.of(Material.WOOD, MaterialColor.WOOD));
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SignGeneratorEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @javax.annotation.Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(level instanceof ServerLevel l && level.getBlockEntity(pos) instanceof SignGeneratorEntity entity) generate(entity, l);
        return InteractionResult.CONSUME;
    }

    public static void generate(SignGeneratorEntity entity, ServerLevel level) {
        if(!tryPlace(entity, level))
            level.setBlock(entity.getBlockPos(), Blocks.AIR.defaultBlockState(), 18);
    }

    private static boolean tryPlace(SignGeneratorEntity entity, ServerLevel serverLevel) {
        if (!Config.Server.worldGen.isVillageGenerationEnabled.get()) return false;
        return VillageSignpost.populate(entity, serverLevel);
    }

}
