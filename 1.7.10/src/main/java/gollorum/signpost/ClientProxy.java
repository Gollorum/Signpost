package gollorum.signpost;

import cpw.mods.fml.client.registry.ClientRegistry;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.render.BlockItemRenderer;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy{

	@Override
	public void init(){
		super.init();
		registerRenderers();
	}
	
	private void registerRenderers(){
		ClientRegistry.bindTileEntitySpecialRenderer(PostPostTile.class, new PostRenderer());
		MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(post), new BlockItemRenderer(new PostPostTile(), new PostRenderer()));
	}

}
