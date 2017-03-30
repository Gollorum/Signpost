package gollorum.signpost;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.blocks.CustomPostPost;
import gollorum.signpost.blocks.CustomPostPostTile;
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
	public CustomPostPost customPost = new CustomPostPost();
	public PostPost post_oak = new PostPost(PostType.OAK);
	public PostPost post_spruce = new PostPost(PostType.SPRUCE);
	public PostPost post_birch = new PostPost(PostType.BIRCH);
	public PostPost post_jungle = new PostPost(PostType.JUNGLE);
	public PostPost post_acacia = new PostPost(PostType.ACACIA);
	public PostPost post_big_oak = new PostPost(PostType.BIGOAK);
	public PostPost post_iron = new PostPost(PostType.IRON);
	public PostPost post_stone = new PostPost(PostType.STONE);
	public PostPost[] posts = {post_oak, post_spruce, post_birch, post_jungle, post_acacia, post_big_oak, post_iron, post_stone};
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
		GameRegistry.registerBlock(customPost, "SignpostCustomPost");
		for(PostPost now: posts){
			GameRegistry.registerBlock(now, "SignpostPost"+now.type.name());
		}
	}
	
	protected void registerTiles(){
		GameRegistry.registerTileEntity(BasePostTile.class, "SignpostBaseTile");
		GameRegistry.registerTileEntity(PostPostTile.class, "SignpostPostTile");
		GameRegistry.registerTileEntity(CustomPostPostTile.class, "SignpostCustomPostTile");
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
