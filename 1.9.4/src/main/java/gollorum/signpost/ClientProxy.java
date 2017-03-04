package gollorum.signpost;

import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.item.Item;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class ClientProxy extends CommonProxy{

	@Override
	void preInit(){
		super.preInit();
		ClientRegistry.bindTileEntitySpecialRenderer(PostPostTile.class, new PostRenderer());
	}
	
	@Override
	void init(){
		super.init();
		BlockHandler.registerRenders();
		ItemHandler.registerRenders();
	}
	
}
