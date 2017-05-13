package gollorum.signpost;

import cpw.mods.fml.client.registry.ClientRegistry;
import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.BigPostPostTile;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.render.BigPostRenderer;
import gollorum.signpost.render.BlockItemRenderer;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

public class BlockHandlerClient extends BlockHandler {

	public void registerRenders(){
		ClientRegistry.bindTileEntitySpecialRenderer(PostPostTile.class, new PostRenderer());
		for(PostPost now: posts){
			PostPostTile tile = new PostPostTile(now.type);
			tile.isItem = true;
			tile.blockType = now;
			MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(now), new BlockItemRenderer(tile, new PostRenderer()));
		}

		ClientRegistry.bindTileEntitySpecialRenderer(BigPostPostTile.class, new BigPostRenderer());
		for(BigPostPost now: bigposts){
			BigPostPostTile tile = new BigPostPostTile(now.type);
			tile.isItem = true;
			tile.blockType = now;
			MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(now), new BlockItemRenderer(tile, new PostRenderer()));
		}
	}
	
}
