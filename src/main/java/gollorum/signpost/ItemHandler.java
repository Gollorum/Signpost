package gollorum.signpost;

import gollorum.signpost.items.CalibratedPostWrench;
import gollorum.signpost.items.PostBrush;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

public class ItemHandler {

	public static final ItemHandler INSTANCE = new ItemHandler();
	
	public static PostWrench tool = new PostWrench();
	public static CalibratedPostWrench calibratedTool = new CalibratedPostWrench();
	public static PostBrush brush = new PostBrush();

	private ItemHandler(){}
	
	public static void init(){}

	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> event) {
		register(event.getRegistry());
	}

	private static void register(IForgeRegistry<Item> registry){
		registerItem(tool, registry);
		registerItem(calibratedTool, registry);
		registerItem(brush, registry);
		registerRecipes();
	}
	
	private static void registerItem(Item item, IForgeRegistry<Item> registry){
		registry.register(item);
	}
	
	public static void registerRenders(){
		registerRender(tool);
		registerRender(calibratedTool);
		registerRender(brush);
	}

	private static void registerRender(Item item){
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
	}

	private static void registerRecipes() {
		if(ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().equals(ConfigHandler.SecurityLevel.ALL) || ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().equals(ConfigHandler.SecurityLevel.OWNERS)){
			GameRegistry.addShapedRecipe(new ResourceLocation("signpost:itemwrenchrecipe"), null, new ItemStack(tool),
					"II",
					"IS",
					"S ",
					'I', Items.IRON_INGOT,
					'S', Items.STICK);
			GameRegistry.addShapelessRecipe(new ResourceLocation("signpost:itemcalibratedwrenchrecipe"), null, new ItemStack(calibratedTool),
					Ingredient.fromItem(tool),
					Ingredient.fromItem(Items.COMPASS));
			GameRegistry.addShapedRecipe(new ResourceLocation("signpost:itemwbrushrecipe"), null, new ItemStack(brush),
									"W",
									"I",
									"S",
									'W', Item.getItemFromBlock(Blocks.WOOL),
									'I', Items.IRON_INGOT,
									'S', Items.STICK);
		}
	}

	public static void register() {}
}
