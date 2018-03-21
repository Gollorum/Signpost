package gollorum.signpost.blocks;

import java.util.UUID;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.BasePostTile;
import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class BasePost extends BlockContainer {

	public BasePost() {
		super(Material.rock);
		this.setHardness(2);
		this.setResistance(100000);
		setBlockName("SignpostBase");
		setCreativeTab(CreativeTabs.tabTransport);
		setBlockTextureName(Signpost.MODID + ":base");
	}

	@Override
	public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
		if (ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
			return false;
		}
		if (!worldIn.isRemote) {
			BaseInfo ws = getWaystoneRootTile(worldIn, x, y, z).getBaseInfo();
			if(ws==null){
				ws = new BaseInfo(generateName(), new MyBlockPos(worldIn, x, y, z, playerIn.dimension), playerIn.getUniqueID());
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
					NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, x, y, z), (EntityPlayerMP) playerIn);
				}
			}
		}
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int p_149915_2_) {
		return new BasePostTile().setup();
	}

	public static BasePostTile getWaystoneRootTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(x, y, z);
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
		BasePostTile tile = getWaystoneRootTile(world, pos.x, pos.y, pos.z);
		String name = generateName();
		UUID owner = player.getUniqueID();
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
		BasePostTile tile = getWaystoneRootTile(world, pos.x, pos.y, pos.z);
		if (tile != null && tile.getBaseInfo() == null) {
            BaseInfo ws = PostHandler.getAllWaystones().getByPos(pos);
            if (ws == null) {
            	UUID owner = player.getUniqueID();
            	PostHandler.getAllWaystones().add(new BaseInfo("", pos, owner));
            }
		}
	}
}
