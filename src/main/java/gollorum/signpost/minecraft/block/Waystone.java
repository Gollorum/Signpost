package gollorum.signpost.minecraft.block;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.Config;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.gui.Colors;
import gollorum.signpost.minecraft.gui.RequestWaystoneGui;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.gui.WaystoneGui;
import gollorum.signpost.networking.PacketHandler;
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
import net.minecraft.loot.LootContext;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Waystone extends Block {

    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final String REGISTRY_NAME = "waystone";

    public static final Waystone INSTANCE = new Waystone();

    private Waystone() {
        super(Properties.create(Material.PISTON, MaterialColor.STONE)
            .hardnessAndResistance(1.5F, 6.0F));
    }

	public static void openGuiIfHasPermission(ServerPlayerEntity player, WorldLocation worldLocation) {
        assert Signpost.getServerType().isServer;
        Optional<WaystoneData> data = WaystoneLibrary.getInstance()
            .getHandleByLocation(worldLocation)
            .map(WaystoneLibrary.getInstance()::getData);
        Optional<PlayerHandle> owner = data.flatMap(d -> d.owner);
        boolean wantsToOpenGui = player.isSneaking() || !data.isPresent();
        boolean mayOpenGui = !owner.isPresent() || owner.get().equals(PlayerHandle.from(player)) || player.getCommandSource().hasPermissionLevel(
            Config.Server.editLockedWaystoneCommandPermissionLevel.get());
        if(wantsToOpenGui && mayOpenGui){
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new RequestWaystoneGui.Package(worldLocation, data));
        } else {
            discover(player, data.get());
        }
	}

	@Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        onRightClick(world, pos, player);
        return ActionResultType.CONSUME;
    }

    public static void onRightClick(World world, BlockPos pos, PlayerEntity player) {
        if(!world.isRemote && player instanceof ServerPlayerEntity)
            openGuiIfHasPermission((ServerPlayerEntity) player, new WorldLocation(pos, world));
    }

    private static void discover(PlayerEntity player, WaystoneData data) {
        assert Signpost.getServerType().isServer;
        if(WaystoneLibrary.getInstance().addDiscovered(new PlayerHandle(player.getUniqueID()), data.handle))
            player.sendMessage(new TranslationTextComponent(LangKeys.discovered, Colors.wrap(data.name, Colors.highlight)), Util.DUMMY_UUID);
    }

    public static void discover(PlayerHandle player, WaystoneData data) {
        assert Signpost.getServerType().isServer;
        if(WaystoneLibrary.getInstance().addDiscovered(player, data.handle)) {
            PlayerEntity playerEntity = Signpost.getServerInstance().getPlayerList().getPlayerByUUID(player.id);
            if(playerEntity != null)
                playerEntity.sendMessage(new TranslationTextComponent(LangKeys.discovered, Colors.wrap(data.name, Colors.highlight)), Util.DUMMY_UUID);
        }
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return getDefaultState().with(FACING, context.getPlacementHorizontalFacing());
    }

    @Override
    public boolean hasTileEntity(BlockState state) { return true; }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new WaystoneTile();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if(world.isRemote) WaystoneGui.display(new WorldLocation(pos, world), Optional.empty());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        return Collections.singletonList(new ItemStack(this.asItem()));
    }

}
