package gollorum.signpost;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BigPostPost;
import gollorum.signpost.blocks.BigPostPost.BigPostType;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPost.PostType;
import gollorum.signpost.blocks.tiles.BaseModelPostTile;
import gollorum.signpost.blocks.tiles.BasePostTile;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.items.CalibratedPostWrench;
import gollorum.signpost.items.PostBrush;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {
	
	public BlockHandler blockHandler;
	ItemHandler itemHandler = new ItemHandler();
	
	public CommonProxy(){
		blockHandler = new BlockHandler();
	}
	
	void preInit(){}

	public void init(){
		blockHandler.register();
		registerTiles();
		itemHandler.register();
		registerRecipes();

		NetworkHandler.register();
		SPEventHandler handler = new SPEventHandler();
		MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
	}
	
	protected void registerTiles(){
		GameRegistry.registerTileEntity(BasePostTile.class, "SignpostBaseTile");
		GameRegistry.registerTileEntity(PostPostTile.class, "SignpostPostTile");
		GameRegistry.registerTileEntity(BigPostPostTile.class, "SignpostBigPostTile");
		GameRegistry.registerTileEntity(BaseModelPostTile.class, "SignpostBaseModelPostTile");
	}

	protected void registerRecipes() {
		blockHandler.registerRecipes();
		itemHandler.registerRecipes();
	}

	public World getWorld(MessageContext ctx){
		return ctx.getServerHandler().playerEntity.worldObj;
	}

	public World getWorld(String worldName, int dim){
		return PostHandler.getWorldByName(worldName, dim);
	}
	
}
