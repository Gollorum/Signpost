package gollorum.signpost;

import java.util.Collection;
import java.util.LinkedList;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
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
		return Minecraft.getMinecraft().theWorld;
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

	@Override
	public World[] getWorlds() {
		return new World[] { FMLClientHandler.instance().getWorldClient() };
	}

	@Override
	public Collection<EntityPlayer> getAllPlayers(){
		LinkedList<EntityPlayer> ret = new LinkedList();
		ret.add(FMLClientHandler.instance().getClientPlayerEntity());
		return ret;
	}
}
