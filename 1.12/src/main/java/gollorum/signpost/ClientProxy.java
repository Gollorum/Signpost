package gollorum.signpost;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientProxy extends CommonProxy{

	public ClientProxy(){
		blockHandler = BlockHandlerClient.INSTANCE;
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
	
}
