package gollorum.signpost;

import java.util.Collection;
import java.util.LinkedList;

import gollorum.signpost.blocks.tiles.BasePostTile;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.management.PlayerStorage;
import gollorum.signpost.management.PlayerStore;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CommonProxy {

	public BlockHandler blockHandler;
	
	public CommonProxy(){
		blockHandler = new BlockHandler();
	}
	
	void preInit(){}
	
	void init(){
		blockHandler.init();
		blockHandler.register();

		ItemHandler.init();
		ItemHandler.register();
		
		registerCapabilities();
		registerTiles();
		
		NetworkHandler.register();
		SPEventHandler handler = SPEventHandler.INSTANCE;
		MinecraftForge.EVENT_BUS.register(handler);
	}

	protected void registerTiles(){
		GameRegistry.registerTileEntity(BasePostTile.class, "SignpostBaseTile");
		GameRegistry.registerTileEntity(PostPostTile.class, "SignpostPostTile");
		GameRegistry.registerTileEntity(BigPostPostTile.class, "SignpostBigPostTile");
	}
	
	protected void registerCapabilities() {
		CapabilityManager.INSTANCE.register(PlayerStore.class, new PlayerStorage(), PlayerStore.class);
	}

	public World getWorld(MessageContext ctx){
		return ctx.getServerHandler().playerEntity.getEntityWorld();
	}
	
	public World getWorld(String worldName, int dim){
		return PostHandler.getWorldByName(worldName, dim);
	}

	public Collection<EntityPlayer> getAllPlayers(){
		LinkedList<EntityPlayer> ret = new LinkedList<EntityPlayer>();
		for(Object now: FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerList()){
			if(now instanceof EntityPlayer){
				ret.add((EntityPlayer) now);
			}
		}
		return ret;
	}
}
