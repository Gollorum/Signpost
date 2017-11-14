package gollorum.signpost;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.render.BigPostRenderer;
import gollorum.signpost.render.BlockItemRenderer;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy{

	public ClientProxy(){
		blockHandler = new BlockHandlerClient();
	}
	
	@Override
	public void init(){
		super.init();
		registerRenderers();
	}
	
	private void registerRenderers(){
		((BlockHandlerClient)blockHandler).registerRenders();
	}

	@Override
	public World getWorld(MessageContext ctx){
		return FMLClientHandler.instance().getWorldClient();
	}
	
	@Override
	public World getWorld(String worldName, int dim){
		if(FMLCommonHandler.instance()!=null && 
			FMLCommonHandler.instance().getMinecraftServerInstance()!=null && 
			FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()){
			return super.getWorld(worldName, dim);
		}else{
			return FMLClientHandler.instance().getWorldClient();
		}
	}
	
}
