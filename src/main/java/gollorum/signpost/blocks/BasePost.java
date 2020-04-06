package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.BasePostTile;
import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

public class BasePost extends BlockContainer {

	public BasePost() {
		super(Material.ROCK);
		this.setHarvestLevel("pickaxe", 1);
		this.setHardness(2);
		this.setResistance(100000);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		this.setTranslationKey("SignpostBase");
		this.setRegistryName(Signpost.MODID+":blockbase");
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if (ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
			return false;
		}
		if (!worldIn.isRemote) {
			BaseInfo ws = getWaystoneRootTile(worldIn, pos).getBaseInfo();
			if(ws==null){
				ws = new BaseInfo(BasePost.generateName(), new MyBlockPos(pos, playerIn.dimension), playerIn.getUniqueID());
				PostHandler.addWaystone(ws);
			}
			if (!playerIn.isSneaking()) {
				if(!PostHandler.doesPlayerKnowNativeWaystone((EntityPlayerMP) playerIn, ws)){
					if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()||ClientConfigStorage.INSTANCE.isDisableDiscovery()) {
						NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.discovered", "<Waystone>", ws.getName()), (EntityPlayerMP) playerIn);
					}
					PostHandler.addDiscovered(playerIn.getUniqueID(), ws);
				}
			} else {
				if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()
						&& ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canUse((EntityPlayerMP) playerIn, ""+ws.owner)) {
					NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, pos.getX(), pos.getY(), pos.getZ()), (EntityPlayerMP) playerIn);
				}
			}
		}
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new BasePostTile().setup();
	}

	public static BasePostTile getWaystoneRootTile(World world, BlockPos pos) {
		TileEntity ret = world.getTileEntity(pos);
		if (ret instanceof BasePostTile) {
			return (BasePostTile) ret;
		} else {
			return null;
		}
	}

	public static String generateName() {
		int i = 1;
		String ret;
		do {
			ret = "Waystone " + (i++);
		} while (PostHandler.getAllWaystones().nameTaken(ret));
		return ret;
	}

	public static void placeServer(World world, MyBlockPos pos, EntityPlayerMP player) {
		BasePostTile tile = getWaystoneRootTile(world, pos.toBlockPos());
		String name = generateName();
		UUID owner = player != null ? player.getUniqueID() : null;
		BaseInfo ws;
		if((ws = tile.getBaseInfo())==null){
			ws = new BaseInfo(name, pos, owner);
			PostHandler.addWaystone(ws);
		}else{
			ws.setAll(new BaseInfo(name, pos, owner));
		}
		PostHandler.addDiscovered(player.getUniqueID(), ws);
		NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.PLACED, world, pos.x, pos.y, pos.z, name));
		NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, pos.x, pos.y, pos.z), player);
	}

	public static void placeClient(final World world, final MyBlockPos pos, final EntityPlayer player) {
		BasePostTile tile = getWaystoneRootTile(world, pos.toBlockPos());
		if (tile != null && tile.getBaseInfo() == null) {
			BaseInfo ws = PostHandler.getAllWaystones().getByPos(pos);
			if (ws == null) {
				UUID owner = player != null ? player.getUniqueID() : null;
				PostHandler.getAllWaystones().add(new BaseInfo("", pos, owner));
			}
		}
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new BasePostTile();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return true;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return true;
	}

	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.SOLID;
	}

	public static void generate(World world, MyBlockPos blockPos, MyBlockPos telePos) {
		String name = generateName();
		generate(world, blockPos, telePos, name);
	}

	public static void generate(World world, MyBlockPos blockPos, MyBlockPos telePos, String name) {
		BasePostTile tile = getWaystoneRootTile(world, blockPos.toBlockPos());
		UUID owner = null;
		BaseInfo ws;
		if ((ws = tile.getBaseInfo()) == null) {
			ws = new BaseInfo(name, blockPos, telePos, owner);
			PostHandler.addWaystone(ws);
		} else {
			ws.setAll(new BaseInfo(name, blockPos, telePos, owner));
		}
		NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.PLACED, world, blockPos.x, blockPos.y, blockPos.z, name));
	}

}
