package gollorum.signpost.blocks;

import java.util.UUID;

import cpw.mods.fml.common.registry.LanguageRegistry;
import gollorum.signpost.SPEventHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.SendDiscoveredToServerMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BlockPos;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public class BasePost extends BlockContainer {

	public BasePost() {
		super(Material.rock);
		this.setHardness(2);
		this.setResistance(100000);
		setBlockName("SignpostBase");
		setCreativeTab(CreativeTabs.tabTransport);
		setBlockTextureName(Signpost.MODID + ":base");
	}

	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (!Signpost.serverSide) {
			BaseInfo ws = getWaystoneRootTile(world, x, y, z).ws;
			if (!player.isSneaking()) {
				String out = LanguageRegistry.instance().getStringLocalization("signpost.discovered");
				if(out.equals("")){
					out = LanguageRegistry.instance().getStringLocalization("signpost.discovered", "en_US");
				}
				out = out.replaceAll("<Waystone>", ws.name);
				Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(out));
				NetworkHandler.netWrap.sendToServer(new SendDiscoveredToServerMessage(ws.name));
			}else{
				player.openGui(Signpost.instance, Signpost.GuiBaseID, world, x, y, z);
			}
		}
		return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int p_149915_2_) {
		return new BasePostTile();
	}

	public static BasePostTile getWaystoneRootTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(x, y, z);
		if (ret instanceof BasePostTile) {
			return (BasePostTile) ret;
		} else {
			return null;
		}
	}

	@Override
	public void onBlockPlacedBy(final World world, final int x, final int y, final int z, final EntityLivingBase entity, ItemStack itemStack) {
		if (entity instanceof EntityPlayerMP) {
			BasePostTile tile = getWaystoneRootTile(world, x, y, z);
			String name = "Waystone" + "" + x + "" + y + "" + z;
			BlockPos pos = new BlockPos(world, x, y, z, entity.dimension);
			UUID owner = entity.getUniqueID();
			tile.ws = new BaseInfo(name, pos, owner);
			if (PostHandler.updateWS(tile.ws, false)) {
				PostHandler.addDiscovered(entity.getUniqueID(), tile.ws);
				NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage().init());
			} else {
				System.out.println("Dies ist ein Fehler und wird deshalb niemals auftreten. Ich bin also nur Einbildung :D");
			}
		} else {
			SPEventHandler.scheduleTask(new Runnable() {
				@Override
				public void run() {
					BasePostTile tile = getWaystoneRootTile(world, x, y, z);
					if(tile!=null&&tile.ws==null){
						String name = "Waystone " + x + "|" + y + "|" + z;
						BlockPos pos = new BlockPos("", x, y, z, entity.dimension);
						UUID owner = entity.getUniqueID();
						for(BaseInfo now: PostHandler.allWaystones){
							if(now.pos.equals(pos)){
								tile.ws = now;
								return;
							}
						}
						tile.ws = new BaseInfo(name, pos, owner);
						PostHandler.updateWS(tile.ws, false);
					}
				}
			}, 20);
		}
	}

	@Override
	public void onBlockPreDestroy(World world, int x, int y, int z, int p_149725_5_) {
		getWaystoneRootTile(world, x, y, z).onBlockDestroy();
		super.onBlockPreDestroy(world, x, y, z, p_149725_5_);
	}

}
