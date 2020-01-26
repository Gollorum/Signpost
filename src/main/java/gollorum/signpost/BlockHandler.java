package gollorum.signpost;

import java.util.ArrayList;

import gollorum.signpost.blocks.BaseModelPost;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.BigPostPost.BigPostType;
import gollorum.signpost.blocks.ItemBlockWithMeta;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.ConfigHandler;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;

public class BlockHandler {
	
	public static BasePost base = new BasePost();
	public static BaseModelPost[] basemodels = BaseModelPost.createAll();
	
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
	
	protected ArrayList<ItemBlockWithMeta>  baseModelItems = new ArrayList<ItemBlockWithMeta>();

	public static final BlockHandler INSTANCE = new BlockHandler();
	
	protected BlockHandler(){}
	
	public static void init(){}

	@SubscribeEvent
	public void registerBlocks(RegistryEvent.Register<Block> event) {
		IForgeRegistry<Block> registry = event.getRegistry();
		registerBlock(base, registry);
		for(PostPost now: posts){
			registerBlock(now, registry);
		}
		for(BigPostPost now: bigposts){
			registerBlock(now, registry);
		}
		for(BaseModelPost now: basemodels){
			registerBlock(now, registry);
		}
	}

	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> event) {
		IForgeRegistry<Item> registry = event.getRegistry();
		registerItem(base, registry);
		for(PostPost now: posts){
			registerItem(now, registry);
		}
		for(BigPostPost now: bigposts){
			registerItem(now, registry);
		}
		for(BaseModelPost now: basemodels){
			registerItem(now, registry);
		}
		registerRecipes();
	}
	
	private void registerBlock(Block block, IForgeRegistry<Block> registry){
		registry.register(block);
	}

	private void registerItem(Block block, IForgeRegistry<Item> registry){
		ItemBlock item = new ItemBlock(block);
		item.setRegistryName(block.getRegistryName());
		registry.register(item);
	}
	
	private void registerRecipes() {
		waystoneRecipe();
		for(PostPost now: posts){
			postRecipe(now);
		}
		for(BigPostPost now: bigposts){
			bigPostRecipe(now);
		}
	}
	
	private void waystoneRecipe(){
		ForgeRegistry<IRecipe> registry = ((ForgeRegistry<IRecipe>)ForgeRegistries.RECIPES);
		registry.remove(new ResourceLocation("signpost:blockbaserecipe"));
		if(ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canCraft && !ClientConfigStorage.INSTANCE.deactivateTeleportation()){
			switch(ClientConfigStorage.INSTANCE.getWaysRec()){
				case EXPENSIVE:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockbaserecipe"), null, new ItemStack(base, 1), 
							"SSS",
							"PPP",
							"SSS",
							'S', Blocks.STONE,
							'P', Items.ENDER_PEARL);
					break;
				case VERY_EXPENSIVE:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockbaserecipe"), null, new ItemStack(base, 1), 
							"SSS",
							" P ",
							"SSS",
							'S', Blocks.STONE,
							'P', Items.NETHER_STAR);
					break;
				case DEACTIVATED:
					break;
				default:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockbaserecipe"), null, new ItemStack(base, 1), 
							"SSS",
							" P ",
							"SSS",
							'S', Blocks.COBBLESTONE,
							'P', Items.ENDER_PEARL);
					break;
			}
			GameRegistry.addShapelessRecipe(new ResourceLocation("signpost:basemodel0recipe"), null, new ItemStack(basemodels[0], 1), Ingredient.fromStacks(new ItemStack(base, 1)));
			for(int i=1; i<basemodels.length; i++){
				GameRegistry.addShapelessRecipe(new ResourceLocation("signpost:basemodel"+i+"recipe"), null, new ItemStack(basemodels[i], 1), Ingredient.fromStacks(new ItemStack(basemodels[i-1], 1)));
			}
			GameRegistry.addShapelessRecipe(new ResourceLocation("signpost:basemodel"+basemodels.length+"recipe"), null, new ItemStack(base, 1), Ingredient.fromStacks(new ItemStack(basemodels[basemodels.length-1], 1)));
		}
	}
	
	private void postRecipe(PostPost post){
		ForgeRegistry<IRecipe> registry = ((ForgeRegistry<IRecipe>)ForgeRegistries.RECIPES);
		registry.remove(new ResourceLocation("signpost:blockpostpostrecipe"+post.type));
		if(ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canCraft){
			switch(ClientConfigStorage.INSTANCE.getSignRec()){
				case EXPENSIVE:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockpostpostrecipe"+post.type), null, new ItemStack(post, 1),
							"A",
							"P",
							"B",
							'A', Items.SIGN,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata),
							'P', Items.ENDER_PEARL);
					break;
				case VERY_EXPENSIVE:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockpostpostrecipe"+post.type), null, new ItemStack(post, 1),
							"A",
							"P",
							"B",
							'A', Items.SIGN,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata),
							'P', Items.NETHER_STAR);
					break;
				case DEACTIVATED:
					break;
				default:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockpostpostrecipe"+post.type), null, new ItemStack(post, 4),
							"A",
							"A",
							"B",
							'A', Items.SIGN,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata));
					break;
			}
		}
	}
	
	private void bigPostRecipe(BigPostPost post){
		ForgeRegistry<IRecipe> registry = ((ForgeRegistry<IRecipe>)ForgeRegistries.RECIPES);
		registry.remove(new ResourceLocation("signpost:blockbigpostrecipe"+post.type));
		if(ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canCraft){
			switch(ClientConfigStorage.INSTANCE.getSignRec()){
				case EXPENSIVE:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockbigpostrecipe"+post.type), null, new ItemStack(post, 1),
							"AAA",
							"APA",
							" B ",
							'A', Items.SIGN,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata),
							'P', Items.ENDER_PEARL);
					break;
				case VERY_EXPENSIVE:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockbigpostrecipe"+post.type), null, new ItemStack(post, 1),
							"AAA",
							"APA",
							" B ",
							'A', Items.SIGN,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata),
							'P', Items.NETHER_STAR);
					break;
				case DEACTIVATED:
					break;
				default:
					GameRegistry.addShapedRecipe(new ResourceLocation("signpost:blockbigpostrecipe"+post.type), null, new ItemStack(post, 4),
							"AAA",
							"AAA",
							" B ",
							'A', Items.SIGN,
							'B', new ItemStack(post.type.baseItem, 1, post.type.metadata));
					break;
			}
		}
	}

	public void register() {}
}
