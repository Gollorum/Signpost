package gollorum.signpost;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;

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
		itemHandler.registerRenders();
	}

	@Override
	public World getWorld(MessageContext ctx){
		return Minecraft.getMinecraft().world;
	}
	
	@Override
	public World getWorld(int dim){
		if(FMLCommonHandler.instance()!=null &&
				FMLCommonHandler.instance().getMinecraftServerInstance()!=null &&
				FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()){
			return super.getWorld(dim);
		}else{
			return FMLClientHandler.instance().getWorldClient();
		}
	}
	
	@Override
	public World[] getWorlds(){
		return new World[]{FMLClientHandler.instance().getWorldClient()};
	}

	
	@Override
	public Collection<EntityPlayer> getAllPlayers(){
		LinkedList<EntityPlayer> ret = new LinkedList();
		ret.add(FMLClientHandler.instance().getClientPlayerEntity());
		return ret;
	}

	public InputStream getResourceInputStream(String location){
		try {
			InputStream ret = FMLClientHandler.instance().getClient().getResourceManager().getResource(new ResourceLocation(location)).getInputStream();
			if(ret != null){
				return ret;
			}
		} catch (IOException e) {}
		return super.getResourceInputStream(location);
	}
}
