package gollorum.signpost;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.network.NetworkHandler;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {

	protected BasePost base = new BasePost();
	protected PostPost post = new PostPost();
	protected PostWrench tool = new PostWrench();

	public void init(){
		System.out.println("init common");
		
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
		GameRegistry.registerBlock(post, "SignpostPost");
	}
	
	protected void registerTiles(){
		GameRegistry.registerTileEntity(BasePostTile.class, "SignpostBaseTile");
		GameRegistry.registerTileEntity(PostPostTile.class, "SignpostPostTile");
	}
	
	protected void registerItems(){
		GameRegistry.registerItem(tool, "SignpostTool");
	}

	protected void registerRecipes() {
		GameRegistry.addRecipe(new ItemStack(base, 4), 
								"SSS",
								" P ",
								"SSS",
								'S', Blocks.cobblestone,
								'P', Items.ender_pearl);
		
		GameRegistry.addRecipe(new ItemStack(post, 4),
								"A",
								"A",
								"B",
								'A', Items.sign,
								'B', Items.stick);
		
		GameRegistry.addRecipe(new ItemStack(tool),
								"II",
								"IS",
								"S ",
								'I', Items.iron_ingot,
								'S', Items.stick);
	}

}
