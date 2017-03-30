package gollorum.signpost;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import gollorum.signpost.blocks.CustomPostPostTile;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.render.BlockItemRenderer;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy{

	@Override
	public void init(){
		super.init();
		registerRenderers();
	}
	
	private void registerRenderers(){
		ClientRegistry.bindTileEntitySpecialRenderer(PostPostTile.class, new PostRenderer(false));
		for(PostPost now: posts){
			PostPostTile tile = new PostPostTile(now.type);
			tile.isItem = true;
			MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(now), new BlockItemRenderer(tile, new PostRenderer(false)));
		}
		ClientRegistry.bindTileEntitySpecialRenderer(CustomPostPostTile.class, new PostRenderer(true));
	}

	@Override
	public World getWorld(MessageContext ctx){
		return FMLClientHandler.instance().getWorldClient();
	}
	
}
