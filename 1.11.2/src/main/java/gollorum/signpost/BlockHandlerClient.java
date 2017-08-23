package gollorum.signpost;

import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.render.BigPostRenderer;
import gollorum.signpost.render.PostRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class BlockHandlerClient extends BlockHandler {

	public void registerRenders(){
		registerRender(base, 0);
		registerRender(basemodel, 0);
		registerRender(basemodel, 1);
		registerRender(basemodel, 2);
		registerRender(basemodel, 3);
		ClientRegistry.bindTileEntitySpecialRenderer(PostPostTile.class, new PostRenderer());
		for(PostPost post: posts){
			registerRender(post, 0);
		}
		ClientRegistry.bindTileEntitySpecialRenderer(BigPostPostTile.class, new BigPostRenderer());
		for(BigPostPost post: bigposts){
			registerRender(post, 0);
		}
	}
	
	public void registerRender(Block block, int meta){
		if(meta==0){
			Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), meta, new ModelResourceLocation(block.getRegistryName(), "inventory"));
		}else{
			Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), meta, new ModelResourceLocation(block.getRegistryName().toString()+meta, "inventory"));
		}
	}

}
