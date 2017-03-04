package gollorum.signpost;

import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.management.PlayerStorage;
import gollorum.signpost.management.PlayerStore;
import gollorum.signpost.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class CommonProxy {

	void preInit(){
		BlockHandler.init();
		BlockHandler.register();

		ItemHandler.init();
		ItemHandler.register();
		
		registerCapabilities();
		registerTiles();
		
		NetworkHandler.register();
		SPEventHandler handler = new SPEventHandler();
		MinecraftForge.EVENT_BUS.register(handler);
	}

	void init() {}

	void postInit() {}

	protected void registerTiles(){
		System.out.println("register tiles");
		GameRegistry.registerTileEntity(BasePostTile.class, "SignpostBaseTile");
		GameRegistry.registerTileEntity(PostPostTile.class, "SignpostPostTile");
	}
	
	protected void registerCapabilities() {
		CapabilityManager.INSTANCE.register(PlayerStore.class, new PlayerStorage(), PlayerStore.class);
	}

}
