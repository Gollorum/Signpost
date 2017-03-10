package gollorum.signpost;

import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.management.ConfigHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlockHandler {

	public BasePost base = new BasePost();
	public PostPost post_oak = new PostPost(PostType.OAK);
	public PostPost post_spruce = new PostPost(PostType.SPRUCE);
	public PostPost post_birch = new PostPost(PostType.BIRCH);
	public PostPost post_jungle = new PostPost(PostType.JUNGLE);
	public PostPost post_acacia = new PostPost(PostType.ACACIA);
	public PostPost post_big_oak = new PostPost(PostType.BIGOAK);
	public PostPost post_iron = new PostPost(PostType.IRON);
	public PostPost post_stone = new PostPost(PostType.STONE);
	public PostPost[] posts = {post_oak, post_spruce, post_birch, post_jungle, post_acacia, post_big_oak, post_iron, post_stone};
	
	public static void init(){
	}
	
	public void register(){
		registerBlock(base);
		for(PostPost now: posts){
			registerBlock(now);
		}
		registerRecipes();
	}
	
	public void registerBlock(Block block){
		GameRegistry.register(block);
		ItemBlock item = new ItemBlock(block);
		item.setRegistryName(block.getRegistryName());
		GameRegistry.register(item);
	}
	
	public void registerRenders(){
		registerRender(base);
		for(PostPost post: posts){
			registerRender(post);
		}
	}
	
	public void registerRender(Block block){
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
	}

	protected void registerRecipes() {
		if(ConfigHandler.securityLevelWaystone.equals(ConfigHandler.SecurityLevel.ALL)&&!ConfigHandler.deactivateTeleportation){
			GameRegistry.addRecipe(new ItemStack(base, 4), 
									"SSS",
									" P ",
									"SSS",
									'S', Blocks.COBBLESTONE,
									'P', Items.ENDER_PEARL);
		}
		if(ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.ALL)){
			for(PostPost now: posts){
				GameRegistry.addRecipe(new ItemStack(now, 4),
										"A",
										"A",
										"B",
										'A', Items.SIGN,
										'B', new ItemStack(now.type.baseItem, 1, now.type.metadata));
			}
		}
	}
}
