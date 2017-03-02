package gollorum.signpost;

import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.items.PostWrench;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ItemHandler {
	
	public static PostWrench tool;

	public static void init(){
		tool = new PostWrench();
	}

	public static void register(){
		registerItem(tool);
	}
	
	public static void registerItem(Item item){
		GameRegistry.register(item);
	}
	
	public static void registerRenders(){
		registerRender(tool);
	}

	public static void registerRender(Item item){
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, 
				new ModelResourceLocation(item.getRegistryName(), "inventory"));
	}

	protected static void registerRecipes() {		
		GameRegistry.addRecipe(new ItemStack(tool),
								"II",
								"IS",
								"S ",
								'I', Items.IRON_INGOT,
								'S', Items.STICK);
	}

}
