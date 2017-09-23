package gollorum.signpost;

import gollorum.signpost.items.CalibratedPostWrench;
import gollorum.signpost.items.PostBrush;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ItemHandler {
	
	public static PostWrench tool = new PostWrench();
	public static CalibratedPostWrench calibratedTool = new CalibratedPostWrench();
	public static PostBrush brush = new PostBrush();

	public static void init(){
		tool = new PostWrench();
		calibratedTool = new CalibratedPostWrench();
		brush = new PostBrush();
	}

	public static void register(){
		registerItem(tool);
		registerItem(calibratedTool);
		registerItem(brush);
		registerRecipes();
	}
	
	public static void registerItem(Item item){
		GameRegistry.register(item);
	}
	
	public static void registerRenders(){
		registerRender(tool);
		registerRender(calibratedTool);
		registerRender(brush);
	}

	public static void registerRender(Item item){
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
	}

	protected static void registerRecipes() {
		if(ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.ALL) || ConfigHandler.securityLevelSignpost.equals(ConfigHandler.SecurityLevel.OWNERS)){
			GameRegistry.addRecipe(new ItemStack(tool),
					"II",
					"IS",
					"S ",
					'I', Items.IRON_INGOT,
					'S', Items.STICK);
			GameRegistry.addShapelessRecipe(new ItemStack(calibratedTool),
					tool,
					Items.COMPASS);
			GameRegistry.addRecipe(new ItemStack(brush),
									"W",
									"I",
									"S",
									'W', Item.getItemFromBlock(Blocks.WOOL),
									'I', Items.IRON_INGOT,
									'S', Items.STICK);
		}
	}

}
