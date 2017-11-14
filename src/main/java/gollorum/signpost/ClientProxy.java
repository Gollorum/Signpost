package gollorum.signpost;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientProxy extends CommonProxy{

	public ClientProxy(){
		blockHandler = BlockHandlerClient.INSTANCE;
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
