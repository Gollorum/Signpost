package gollorum.signpost;

import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.BigPostPost.BigPostType;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.management.ConfigHandler;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlockHandler {

	public static BasePost base = new BasePost();
	
	public static final PostPost post_oak = new PostPost(PostType.OAK);
	public static final PostPost post_spruce = new PostPost(PostType.SPRUCE);
	public static final PostPost post_birch = new PostPost(PostType.BIRCH);
	public static final PostPost post_jungle = new PostPost(PostType.JUNGLE);
	public static final PostPost post_acacia = new PostPost(PostType.ACACIA);
	public static final PostPost post_big_oak = new PostPost(PostType.BIGOAK);
	public static final PostPost post_iron = new PostPost(PostType.IRON);
	public static final PostPost post_stone = new PostPost(PostType.STONE);
	public static final PostPost[] posts = {post_oak, post_spruce, post_birch, post_jungle, post_acacia, post_big_oak, post_iron, post_stone};

	public static final BigPostPost bigpost_oak = new BigPostPost(BigPostType.OAK);
	public static final BigPostPost bigpost_spruce = new BigPostPost(BigPostType.SPRUCE);
	public static final BigPostPost bigpost_birch = new BigPostPost(BigPostType.BIRCH);
	public static final BigPostPost bigpost_jungle = new BigPostPost(BigPostType.JUNGLE);
	public static final BigPostPost bigpost_acacia = new BigPostPost(BigPostType.ACACIA);
	public static final BigPostPost bigpost_big_oak = new BigPostPost(BigPostType.BIGOAK);
	public static final BigPostPost bigpost_iron = new BigPostPost(BigPostType.IRON);
	public static final BigPostPost bigpost_stone = new BigPostPost(BigPostType.STONE);
	public static final BigPostPost[] bigposts = {bigpost_oak, bigpost_spruce, bigpost_birch, bigpost_jungle, bigpost_acacia, bigpost_big_oak, bigpost_iron, bigpost_stone};

	public static void init(){
	}
	
	public void register(){
		registerBlock(base);
		for(PostPost now: posts){
			registerBlock(now);
		}
		for(BigPostPost now: bigposts){
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
	
	protected void registerRecipes() {
		if(ConfigHandler.securityLevelWaystone.equals(ConfigHandler.SecurityLevel.ALL)&&!ConfigHandler.deactivateTeleportation){
			GameRegistry.addRecipe(new ItemStack(base, 1), 
									"SSS",
									" P ",
									"SSS",
									'S', Blocks.COBBLESTONE,
									'P', Items.ENDER_PEARL);
		}
		if(ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.ALL) || ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.OWNERS)){
			for(PostPost now: posts){
				GameRegistry.addRecipe(new ItemStack(now, 4),
										"A",
										"A",
										"B",
										'A', Items.SIGN,
										'B', new ItemStack(now.type.baseItem, 1, now.type.metadata));
			}
			for(BigPostPost now: bigposts){
				GameRegistry.addRecipe(new ItemStack(now, 4),
										"AAA",
										"AAA",
										" B ",
										'A', Items.SIGN,
										'B', new ItemStack(now.type.baseItem, 1, now.type.metadata));
			}
		}
	}
}
