package gollorum.signpost;

import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.PostPost;
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

	public static Block base;
	public static Block post;
	
	public static void init(){
		base = new BasePost();
		post = new PostPost();
	}
	
	public static void register(){
		registerBlock(base);
		registerBlock(post);
		registerRecipes();
	}
	
	public static void registerBlock(Block block){
		GameRegistry.register(block);
		ItemBlock item = new ItemBlock(block);
		item.setRegistryName(block.getRegistryName());
		GameRegistry.register(item);
	}
	
	public static void registerRenders(){
		registerRender(base);
		registerRender(post);
	}
	
	public static void registerRender(Block block){
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), 0, 
				new ModelResourceLocation(block.getRegistryName(), "inventory"));
	}

	protected static void registerRecipes() {
		GameRegistry.addRecipe(new ItemStack(base, 4), 
								"SSS",
								" P ",
								"SSS",
								'S', Blocks.COBBLESTONE,
								'P', Items.ENDER_PEARL);
		
		GameRegistry.addRecipe(new ItemStack(post, 4),
								"A",
								"A",
								"B",
								'A', Items.SIGN,
								'B', Items.STICK);
	}

}
