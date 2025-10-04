package gollorum.signpost.minecraft.block;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.*;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.data.WaystoneHandleData;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TextComponents;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.security.WithOwner;
import gollorum.signpost.utils.IDelay;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;

public class WaystoneBlock extends BaseEntityBlock {

    public static WaystoneBlock createInstance() {
        assert instance == null;
        return instance = new WaystoneBlock();
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String REGISTRY_NAME = "waystone";

    private static WaystoneBlock instance = null;

    public static WaystoneBlock getInstance() {
        assert instance != null;
        return instance;
    }

    private WaystoneBlock() {
        this(Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, REGISTRY_NAME)))
            .mapColor(MapColor.STONE)
            .instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops()
            .strength(1.5F, 6.0F)
        );
    }

    private WaystoneBlock(Properties properties) {
        super(properties);
        instance = this;
    }

	public static void openGuiIfHasPermission(ServerPlayer player, WorldLocation worldLocation) {
        assert Signpost.getServerType().isServer;
        Optional<WaystoneData> data = WaystoneLibrary.getInstance()
            .getHandleByLocation(worldLocation)
            .flatMap(WaystoneLibrary.getInstance()::getData);
        boolean wantsToOpenGui = data.isEmpty()
            || WaystoneLibrary.getInstance().isDiscovered(PlayerHandle.from(player), data.get().handle());
        boolean mayOpenGui = data.map(d -> d.hasThePermissionToEdit(player)).orElse(true);
        if(wantsToOpenGui && mayOpenGui){
            PacketHandler.getInstance().sendToPlayer(player, new RequestWaystoneGui.Package(worldLocation, data));
        } else {
            discover(player, data.get());
        }
	}

    @Override
    protected InteractionResult useItemOn(ItemStack p_316304_, BlockState p_316362_, Level level, BlockPos pos, Player player, InteractionHand p_316595_, BlockHitResult p_316140_) {
        onRightClick(level, pos, player);
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        onRightClick(level, pos, player);
        return InteractionResult.CONSUME;
    }

    public static void onRightClick(Level world, BlockPos pos, Player player) {
        if(!world.isClientSide() && player instanceof ServerPlayer)
            openGuiIfHasPermission((ServerPlayer) player, new WorldLocation(pos, world));
    }

    private static void discover(ServerPlayer player, WaystoneData data) {
        if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(player.getUUID()), data.handle()))
            player.sendSystemMessage(Component.translatable(LangKeys.discovered, TextComponents.waystone(player, data.name())));
    }

    public static void discover(PlayerHandle player, WaystoneData data) {
        assert Signpost.getServerType().isServer;
        if(WaystoneLibrary.getInstance().addDiscovered(player, data.handle())) {
            ServerPlayer playerEntity = player.asEntity();
            if(playerEntity != null)
                playerEntity.sendSystemMessage(Component.translatable(LangKeys.discovered, TextComponents.waystone(playerEntity, data.name())));
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

    public static void registerOwnerAndRequestGui(Level world, BlockPos pos, LivingEntity placer, ItemStack currentStack) {
        var stack = currentStack.copy(); // stack might be changed in the delay (set block -> block no longer in inventory)
        IDelay.forFrames(6, world.isClientSide(), () ->
            TileEntityUtils.delayUntilTileEntityExists(world, pos, WaystoneTile.getBlockEntityType(), t -> {
                var hasName = registerOwnerAndSeeIfHasName(t, world, pos, placer, stack);
                if (!hasName && placer instanceof ServerPlayer sp)
                    PacketHandler.getInstance().sendToPlayer(
                        sp,
                        new RequestWaystoneGui.Package(new WorldLocation(pos, world), Optional.empty())
                    );
            }, 100, Optional.empty()));
    }

    public static boolean registerOwnerAndSeeIfHasName(
        BlockEntity tileEntity,
        Level world,
        BlockPos pos,
        LivingEntity placer,
        ItemStack stack
    ) {
        if (tileEntity instanceof WithOwner.OfWaystone waystoneTile)
            waystoneTile.setWaystoneOwner(Optional.of(PlayerHandle.from(placer)));
        if (placer instanceof ServerPlayer sp) {
            WorldLocation worldLocation = new WorldLocation(pos, world);
            boolean wasRegistered = getCustomName(stack).map(name -> {
                WaystoneLocationData locationData = new WaystoneLocationData(worldLocation, Vector3.fromVec3d(placer.position()));
                var handleTag = stack.get(WaystoneHandleData.TYPE);
                Optional<WaystoneHandle.Vanilla> handle = handleTag != null
                    ? Optional.of(handleTag.handle())
                    : Optional.empty();
                return WaystoneLibrary.getInstance().tryAddNew(name, locationData, sp, handle);
            }).orElse(false);
            return wasRegistered;
        } else return false;
    }

    // Modified copy of ItemStack.getHoverName()
    private static Optional<String> getCustomName(ItemStack stack) {
        var component = stack.getCustomName();
        if (component != null) {
            return Optional.of(component.getString());
        } else {
            return Optional.empty();
        }
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
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        var itemStack = super.getCloneItemStack(level, pos, state, includeData);
        if (includeData)
            itemStack = fillClonedItemStack(itemStack, level, pos);
        return itemStack;
    }

    public static ItemStack fillClonedItemStack(ItemStack stack, LevelReader level, BlockPos pos) {
        BlockEntity untypedEntity = level.getBlockEntity(pos);
        if(untypedEntity instanceof WaystoneTile) {
            WaystoneTile tile = (WaystoneTile) untypedEntity;

            tile.getHandle().ifPresent(h -> stack.set(WaystoneHandleData.TYPE, new WaystoneHandleData(h)));
            tile.getName().ifPresent(n -> stack.set(DataComponents.CUSTOM_NAME, Component.literal(n)));
        }
        return stack;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(WaystoneBlock::new);
    }
}
