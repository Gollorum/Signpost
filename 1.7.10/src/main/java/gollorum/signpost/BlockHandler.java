package gollorum.signpost;

import java.util.ArrayList;

import cpw.mods.fml.common.registry.GameRegistry;
import gollorum.signpost.blocks.BaseModelPost;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.BigPostPost.BigPostType;
import gollorum.signpost.blocks.ItemBlockWithMeta;
import gollorum.signpost.blocks.ItemBlockWithMetaFacing;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.management.ConfigHandler;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

public class BlockHandler {

	public static BasePost base = new BasePost();
	public static BaseModelPost[] basemodels = BaseModelPost.createAll();

	public PostPost post_oak = new PostPost(PostType.OAK);
	public PostPost post_spruce = new PostPost(PostType.SPRUCE);
	public PostPost post_birch = new PostPost(PostType.BIRCH);
	public PostPost post_jungle = new PostPost(PostType.JUNGLE);
	public PostPost post_acacia = new PostPost(PostType.ACACIA);
	public PostPost post_big_oak = new PostPost(PostType.BIGOAK);
	public PostPost post_iron = new PostPost(PostType.IRON);
	public PostPost post_stone = new PostPost(PostType.STONE);
	public PostPost[] posts = {post_oak, post_spruce, post_birch, post_jungle, post_acacia, post_big_oak, post_iron, post_stone};

	public BigPostPost bigpost_oak = new BigPostPost(BigPostType.OAK);
	public BigPostPost bigpost_spruce = new BigPostPost(BigPostType.SPRUCE);
	public BigPostPost bigpost_birch = new BigPostPost(BigPostType.BIRCH);
	public BigPostPost bigpost_jungle = new BigPostPost(BigPostType.JUNGLE);
	public BigPostPost bigpost_acacia = new BigPostPost(BigPostType.ACACIA);
	public BigPostPost bigpost_big_oak = new BigPostPost(BigPostType.BIGOAK);
	public BigPostPost bigpost_iron = new BigPostPost(BigPostType.IRON);
	public BigPostPost bigpost_stone = new BigPostPost(BigPostType.STONE);
	public BigPostPost[] bigposts = {bigpost_oak, bigpost_spruce, bigpost_birch, bigpost_jungle, bigpost_acacia, bigpost_big_oak, bigpost_iron, bigpost_stone};

	protected ArrayList<ItemBlockWithMeta>  baseModelItems = new ArrayList<ItemBlockWithMeta>();

	public void register(){
		GameRegistry.registerBlock(base, "SignpostBase");
		for(BaseModelPost basemodel: basemodels){
			GameRegistry.registerBlock(basemodel, ItemBlockWithMetaFacing.class, "blockbasemodel"+basemodel.type.getID());
		}
		for(PostPost now: posts){
			GameRegistry.registerBlock(now, "SignpostPost"+now.type.name());
		}
		for(BigPostPost now: bigposts){
			GameRegistry.registerBlock(now, "BigSignpostPost"+now.type.name());
		}
	}
	
	public void registerRecipes() {
		waystoneRecipe();
		for(PostPost now: posts){
			postRecipe(now);
		}
		for(BigPostPost now: bigposts){
			bigPostRecipe(now);
		}
	}

	private void waystoneRecipe(){
		for(Object now: CraftingManager.getInstance().getRecipeList()){
			if(now==null ||! (now instanceof IRecipe) || ((IRecipe)now).getRecipeOutput()==null || ((IRecipe)now).getRecipeOutput().getItem()==null){
				continue;
			}
			if(((IRecipe)now).getRecipeOutput().getItem().equals(Item.getItemFromBlock(base))){
				CraftingManager.getInstance().getRecipeList().remove(now);
				break;
			}
		}
		if(ConfigHandler.securityLevelWaystone.equals(ConfigHandler.SecurityLevel.ALL)&&!ConfigHandler.deactivateTeleportation){
			switch(ConfigHandler.waysRec){
				case EXPENSIVE:
					GameRegistry.addRecipe(new ItemStack(base, 1),
								"SSS",
								"PPP",
								"SSS",
								'S', Blocks.stone,
								'P', Items.ender_pearl);
					break;
				case VERY_EXPENSIVE:
					GameRegistry.addRecipe(new ItemStack(base, 1),
							"SSS",
							" P ",
							"SSS",
							'S', Blocks.stone,
							'P', Items.nether_star);
					break;
				case DEACTIVATED:
					break;
				default:
					GameRegistry.addRecipe(new ItemStack(base, 1),
							"SSS",
							" P ",
							"SSS",
							'S', Blocks.cobblestone,
							'P', Items.ender_pearl);
					break;
				}
				GameRegistry.addShapelessRecipe(new ItemStack(basemodels[0], 1), base);
				for(int i=1; i<basemodels.length; i++){
					GameRegistry.addShapelessRecipe(new ItemStack(basemodels[i], 1), basemodels[i-1]);
				}
				GameRegistry.addShapelessRecipe(new ItemStack(base, 1), basemodels[basemodels.length-1]);
		}
	}
	private void postRecipe(PostPost post){
		for(Object now: CraftingManager.getInstance().getRecipeList()){
			if(now==null ||! (now instanceof IRecipe) || ((IRecipe)now).getRecipeOutput()==null || ((IRecipe)now).getRecipeOutput().getItem()==null){
				continue;
			}
			if(((IRecipe)now).getRecipeOutput().getItem().equals(Item.getItemFromBlock(post))){
				CraftingManager.getInstance().getRecipeList().remove(now);
				break;
			}
		}
		if(ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.ALL) || ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.OWNERS)){
			switch(ConfigHandler.signRec){
				case EXPENSIVE:
					GameRegistry.addRecipe(new ItemStack(post, 1),
							"A",
							"P",
							"B",
							'A', Items.sign,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata),
							'P', Items.ender_pearl);
					break;
				case VERY_EXPENSIVE:
					GameRegistry.addRecipe(new ItemStack(post, 1),
							"A",
							"P",
							"B",
							'A', Items.sign,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata),
							'P', Items.nether_star);
					break;
				case DEACTIVATED:
					break;
				default:
					GameRegistry.addRecipe(new ItemStack(post, 4),
							"A",
							"A",
							"B",
							'A', Items.sign,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata));
					break;
			}
		}
	}

	private void bigPostRecipe(BigPostPost post){
		for(Object now: CraftingManager.getInstance().getRecipeList()){
			if(now==null ||! (now instanceof IRecipe) || ((IRecipe)now).getRecipeOutput()==null || ((IRecipe)now).getRecipeOutput().getItem()==null){
				continue;
			}
			if(((IRecipe)now).getRecipeOutput().getItem().equals(Item.getItemFromBlock(post))){
				CraftingManager.getInstance().getRecipeList().remove(now);
				break;
			}
		}
		if(ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.ALL) || ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.OWNERS)){
			switch(ConfigHandler.signRec){
			case EXPENSIVE:
				GameRegistry.addRecipe(new ItemStack(post, 1),
						"AAA",
						"APA",
						" B ",
						'A', Items.sign,
						'B', new ItemStack(post.type.baseItem, 1, post.type.metadata),
						'P', Items.ender_pearl);
				break;
			case VERY_EXPENSIVE:
				GameRegistry.addRecipe(new ItemStack(post, 1),
						"AAA",
						"APA",
						" B ",
						'A', Items.sign,
						'B', new ItemStack(post.type.baseItem, 1, post.type.metadata),
						'P', Items.nether_star);
				break;
			case DEACTIVATED:
				break;
			default:
				GameRegistry.addRecipe(new ItemStack(post, 4),
						"AAA",
						"AAA",
						" B ",
						'A', Items.sign,
						'B', new ItemStack(post.type.baseItem, 1, post.type.metadata));
				
				
				
				
				
				break;
			}
		}
	}
}
