package gollorum.signpost;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.BigPostPost.BigPostType;
import gollorum.signpost.blocks.BigPostPostTile;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.network.NetworkHandler;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {

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

	/*public BigPostPost bigpost_oak = new BigPostPost(BigPostType.OAK);
	public BigPostPost bigpost_spruce = new BigPostPost(BigPostType.SPRUCE);
	public BigPostPost bigpost_birch = new BigPostPost(BigPostType.BIRCH);
	public BigPostPost bigpost_jungle = new BigPostPost(BigPostType.JUNGLE);
	public BigPostPost bigpost_acacia = new BigPostPost(BigPostType.ACACIA);
	public BigPostPost bigpost_big_oak = new BigPostPost(BigPostType.BIGOAK);
	public BigPostPost bigpost_iron = new BigPostPost(BigPostType.IRON);
	public BigPostPost bigpost_stone = new BigPostPost(BigPostType.STONE);
	public BigPostPost[] bigposts = {bigpost_oak, bigpost_spruce, bigpost_birch, bigpost_jungle, bigpost_acacia, bigpost_big_oak, bigpost_iron, bigpost_stone};
	*/
	public PostWrench tool = new PostWrench();

	public void init(){
		registerBlocks();
		registerTiles();
		registerItems();
		registerRecipes();

		NetworkHandler.register();
		SPEventHandler handler = new SPEventHandler();
		MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
	}

	protected void registerBlocks(){
		GameRegistry.registerBlock(base, "SignpostBase");
		for(PostPost now: posts){
			GameRegistry.registerBlock(now, "SignpostPost"+now.type.name());
		}
		/*for(BigPostPost now: bigposts){
			GameRegistry.registerBlock(now, "BigSignpostPost"+now.type.name());
		}*/
	}
	
	protected void registerTiles(){
		GameRegistry.registerTileEntity(BasePostTile.class, "SignpostBaseTile");
		GameRegistry.registerTileEntity(PostPostTile.class, "SignpostPostTile");
//		GameRegistry.registerTileEntity(BigPostPostTile.class, "SignpostBigPostTile");
	}
	
	protected void registerItems(){
		GameRegistry.registerItem(tool, "SignpostTool");
	}

	protected void registerRecipes() {
		if(ConfigHandler.securityLevelWaystone.equals(ConfigHandler.SecurityLevel.ALL)&&!ConfigHandler.deactivateTeleportation){
			GameRegistry.addRecipe(new ItemStack(base, 4), 
									"SSS",
									" P ",
									"SSS",
									'S', Blocks.cobblestone,
									'P', Items.ender_pearl);
		}
		if(ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.ALL)){
			for(PostPost now: posts){
				GameRegistry.addRecipe(new ItemStack(now, 4),
										"A",
										"A",
										"B",
										'A', Items.sign,
										'B', new ItemStack(now.type.baseItem, 1, now.type.metadata));
			}
			/*for(BigPostPost now: bigposts){
				GameRegistry.addRecipe(new ItemStack(now, 4),
										"AAA",
										"AAA",
										" B ",
										'A', Items.sign,
										'B', new ItemStack(now.type.baseItem, 1, now.type.metadata));
			}*/
			GameRegistry.addRecipe(new ItemStack(tool),
									"II",
									"IS",
									"S ",
									'I', Items.iron_ingot,
									'S', Items.stick);
		}
	}

	public World getWorld(MessageContext ctx){
		return ctx.getServerHandler().playerEntity.worldObj;
	}
	
}
