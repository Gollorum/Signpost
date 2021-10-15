package gollorum.signpost.minecraft.block;

import gollorum.signpost.*;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithCountRestriction;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Optional;

public class WaystoneBlock extends Block implements WithCountRestriction {

    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String REGISTRY_NAME = "waystone";

    public static final WaystoneBlock INSTANCE = new WaystoneBlock();

    private WaystoneBlock() {
        super(Properties.of(Material.PISTON, MaterialColor.STONE)
            .strength(1.5F, 6.0F)
            .requiresCorrectToolForDrops()
        );
    }

	public static void openGuiIfHasPermission(ServerPlayerEntity player, WorldLocation worldLocation) {
        assert Signpost.getServerType().isServer;
        Optional<WaystoneData> data = WaystoneLibrary.getInstance()
            .getHandleByLocation(worldLocation)
            .map(WaystoneLibrary.getInstance()::getData);
        boolean wantsToOpenGui = !data.isPresent()
            || WaystoneLibrary.getInstance().isDiscovered(PlayerHandle.from(player), data.get().handle);
        boolean mayOpenGui = data.map(d -> d.hasThePermissionToEdit(player)).orElse(true);
        if(wantsToOpenGui && mayOpenGui){
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new RequestWaystoneGui.Package(worldLocation, data));
        } else {
            discover(player, data.get());
        }
	}

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        onRightClick(world, pos, player);
        return ActionResultType.CONSUME;
    }

    public static void onRightClick(World world, BlockPos pos, PlayerEntity player) {
        if(!world.isClientSide() && player instanceof ServerPlayerEntity)
            openGuiIfHasPermission((ServerPlayerEntity) player, new WorldLocation(pos, world));
    }

    private static void discover(ServerPlayerEntity player, WaystoneData data) {
        if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(player.getUUID()), data.handle))
            player.sendMessage(new TranslationTextComponent(LangKeys.discovered, TextComponents.waystone(player, data.name)), Util.NIL_UUID);
    }

    public static void discover(PlayerHandle player, WaystoneData data) {
        assert Signpost.getServerType().isServer;
        if(WaystoneLibrary.getInstance().addDiscovered(player, data.handle)) {
            ServerPlayerEntity playerEntity = player.asEntity();
            if(playerEntity != null)
                playerEntity.sendMessage(new TranslationTextComponent(LangKeys.discovered, TextComponents.waystone(playerEntity, data.name)), Util.NIL_UUID);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
        registerOwnerAndRequestGui(world, pos, placer);
    }

    public static void registerOwnerAndRequestGui(World world, BlockPos pos, LivingEntity placer) {
        Delay.forFrames(6, world.isClientSide(), () ->
            TileEntityUtils.delayUntilTileEntityExists(world, pos, WaystoneTile.class, t -> {
                t.setWaystoneOwner(Optional.of(PlayerHandle.from(placer)));
                if(placer instanceof ServerPlayerEntity) PacketHandler.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) placer),
                    new RequestWaystoneGui.Package(new WorldLocation(pos, world), Optional.empty())
                );
            }, 100, Optional.empty()));
    }

    @Override
    public BlockRestrictions.Type getBlockRestrictionType() {
        return BlockRestrictions.Type.Waystone;
    }

}
