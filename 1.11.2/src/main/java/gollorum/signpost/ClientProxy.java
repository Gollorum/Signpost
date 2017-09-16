package gollorum.signpost;

import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientProxy extends CommonProxy{

	public ClientProxy(){
		blockHandler = new BlockHandlerClient();
	}
	
	@Override
	void preInit(){
		super.preInit();
		OBJLoader.INSTANCE.addDomain("signpost");
	}
	
	@Override
	void init(){
		super.init();
		((BlockHandlerClient)blockHandler).registerRenders();
		ItemHandler.registerRenders();
	}

	@Override
	public World getWorld(MessageContext ctx){
		return Minecraft.getMinecraft().world;
	}
	
	@Override
	public World getWorld(String worldName){
		return FMLClientHandler.instance().getWorldClient();
	}
	
}
