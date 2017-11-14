package gollorum.signpost;

import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.BigPostPostTile;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.render.BigPostRenderer;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class BlockHandlerClient extends BlockHandler {

	public void registerRenders(){
		registerRender(base);
		ClientRegistry.bindTileEntitySpecialRenderer(PostPostTile.class, new PostRenderer());
		for(PostPost post: posts){
			registerRender(post);
		}
		ClientRegistry.bindTileEntitySpecialRenderer(BigPostPostTile.class, new BigPostRenderer());
		for(BigPostPost post: bigposts){
			registerRender(post);
		}
	}
	
	public void registerRender(Block block){
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
	}

}
