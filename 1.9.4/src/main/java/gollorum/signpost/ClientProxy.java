package gollorum.signpost;

import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientProxy extends CommonProxy{

	@Override
	void preInit(){
		super.preInit();
		ClientRegistry.bindTileEntitySpecialRenderer(PostPostTile.class, new PostRenderer());
	}
	
	@Override
	void init(){
		super.init();
		this.blockHandler.registerRenders();
		ItemHandler.registerRenders();
	}

	@Override
	public World getWorld(MessageContext ctx){
		return Minecraft.getMinecraft().theWorld;
	}
	
}
