package gollorum.signpost.minecraft.block;

import gollorum.signpost.*;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithCountRestriction;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Optional;

public class WaystoneBlock extends BaseEntityBlock implements WithCountRestriction {

    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String REGISTRY_NAME = "waystone";

    private static WaystoneBlock instance = null;
    public static WaystoneBlock createInstance() {
        assert instance == null;
        return instance = new WaystoneBlock();
    }

    public static WaystoneBlock getInstance() {
        assert instance != null;
        return instance;
    }


    private WaystoneBlock() {
        super(Properties.of(Material.PISTON, MaterialColor.STONE)
            .strength(1.5F, 6.0F)
            .requiresCorrectToolForDrops()
        );
    }

	public static void openGuiIfHasPermission(ServerPlayer player, WorldLocation worldLocation) {
        assert Signpost.getServerType().isServer;
        Optional<WaystoneData> data = WaystoneLibrary.getInstance()
            .getHandleByLocation(worldLocation)
            .flatMap(WaystoneLibrary.getInstance()::getData);
        boolean wantsToOpenGui = data.isEmpty()
            || WaystoneLibrary.getInstance().isDiscovered(PlayerHandle.from(player), data.get().handle);
        boolean mayOpenGui = data.map(d -> d.hasThePermissionToEdit(player)).orElse(true);
        if(wantsToOpenGui && mayOpenGui){
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new RequestWaystoneGui.Package(worldLocation, data));
        } else {
            discover(player, data.get());
        }
	}

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        onRightClick(world, pos, player);
        return InteractionResult.CONSUME;
    }

    public static void onRightClick(Level world, BlockPos pos, Player player) {
        if(!world.isClientSide() && player instanceof ServerPlayer)
            openGuiIfHasPermission((ServerPlayer) player, new WorldLocation(pos, world));
    }

    private static void discover(ServerPlayer player, WaystoneData data) {
        if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(player.getUUID()), data.handle))
            player.sendSystemMessage(Component.translatable(LangKeys.discovered, TextComponents.waystone(player, data.name)));
    }

    public static void discover(PlayerHandle player, WaystoneData data) {
        assert Signpost.getServerType().isServer;
        if(WaystoneLibrary.getInstance().addDiscovered(player, data.handle)) {
            ServerPlayer playerEntity = player.asEntity();
            if(playerEntity != null)
                playerEntity.sendSystemMessage(Component.translatable(LangKeys.discovered, TextComponents.waystone(playerEntity, data.name)));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaystoneTile(pos, state);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        registerOwnerAndRequestGui(world, pos, placer, stack);
    }

    public static void registerOwnerAndRequestGui(Level world, BlockPos pos, LivingEntity placer, ItemStack stack) {
        Delay.forFrames(6, world.isClientSide(), () ->
            TileEntityUtils.delayUntilTileEntityExists(world, pos, WaystoneTile.getBlockEntityType(), t -> {
                t.setWaystoneOwner(Optional.of(PlayerHandle.from(placer)));
                if(placer instanceof ServerPlayer) {
                    WorldLocation worldLocation = new WorldLocation(pos, world);
                    boolean wasRegistered = getCustomName(stack).map(name -> {
                        WaystoneLocationData locationData = new WaystoneLocationData(worldLocation, Vector3.fromVec3d(placer.position()));
                        CompoundTag handleTag = stack.getTagElement("Handle");
                        Optional<WaystoneHandle.Vanilla> handle = handleTag != null ? Optional.of(WaystoneHandle.Vanilla.Serializer.read(handleTag)) : Optional.empty();
                        return WaystoneLibrary.getInstance().tryAddNew(name, locationData, (ServerPlayer) placer, handle);
                    }).orElse(false);
                    if(!wasRegistered)
                        PacketHandler.send(
                            PacketDistributor.PLAYER.with(() -> (ServerPlayer) placer),
                            new RequestWaystoneGui.Package(worldLocation, Optional.empty())
                        );
                }
            }, 100, Optional.empty()));
    }

    // Modified copy of ItemStack.getHoverName()
    private static Optional<String> getCustomName(ItemStack stack) {
        CompoundTag displayTag = stack.getTagElement("display");
        if (displayTag != null && displayTag.contains("Name", 8)) {
            try {
                Component component = Component.Serializer.fromJson(displayTag.getString("Name"));
                if (component != null) {
                    return Optional.of(component.getString());
                }

            } catch (Exception ignored) {}
        }
        return Optional.empty();
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

    // Does this work with silk touch?
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return fillClonedItemStack(super.getCloneItemStack(state, target, level, pos, player), level, pos, player);
    }

    public static ItemStack fillClonedItemStack(ItemStack stack, BlockGetter level, BlockPos pos, Player player) {
        BlockEntity untypedEntity = level.getBlockEntity(pos);
        if(untypedEntity instanceof WaystoneTile) {
            WaystoneTile tile = (WaystoneTile) untypedEntity;
            if(player.hasPermissions(Config.Server.permissions.pickUnownedWaystonePermissionLevel.get())
                || tile.getWaystoneOwner().map(o -> o.equals(PlayerHandle.from(player))).orElse(true)) {

                tile.getHandle().ifPresent(h -> stack.addTagElement("Handle", WaystoneHandle.Vanilla.Serializer.write(h)));
                tile.getName().ifPresent(n -> stack.setHoverName(Component.literal(n)));
            }
        }
        return stack;
    }
}
