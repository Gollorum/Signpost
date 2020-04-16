package gollorum.signpost;

import gollorum.signpost.commands.*;
import gollorum.signpost.gui.SignGuiHandler;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.worldGen.villages.NameLibrary;
import gollorum.signpost.worldGen.villages.VillageLibrary;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Signpost.MODID, version = Signpost.VERSION, name = "SignPost")
public class Signpost{

	@Instance
	public static Signpost instance;
	public static final String MODID = "signpost";
	public static final String VERSION = "1.08.5";

	public static final int GuiBaseID = 0;
	public static final int GuiPostID = 1;
	public static final int GuiBigPostID = 2;
	public static final int GuiPostBrushID = 3;
	public static final int GuiPostRotationID = 4;

	public static File configFile;
	public static File configFolder;
	
	public static NBTTagCompound saveFile;
	public static final Logger LOG = LogManager.getLogger(MODID); 
	
	@SidedProxy(clientSide = "gollorum.signpost.ClientProxy", serverSide = "gollorum.signpost.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preinit(FMLPreInitializationEvent event) {

		configFolder = new File(event.getModConfigurationDirectory() + "/" + MODID);
		configFolder.mkdirs();
		configFile = new File(configFolder.getPath(), MODID + ".cfg");
		ConfigHandler.init(configFile);
		NameLibrary.init(configFolder.getPath()); 
		proxy.preInit();
        
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new SignGuiHandler());
		proxy.init();
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event){
		ConfigHandler.postInit();
	}
	
	@EventHandler
	public void serverAboutToStart(FMLServerAboutToStartEvent e){
		PostHandler.init();
		VillageLibrary.init();
	}
    
	@EventHandler
	public void serverStarting(FMLServerStartingEvent e) {
		registerCommands((ServerCommandManager) e.getServer().getCommandManager());
		ConfigHandler.init(configFile);
	}
	
	private void registerCommands(ServerCommandManager manager) {
		manager.registerCommand(new ConfirmTeleportCommand());
		manager.registerCommand(new GetWaystoneCount());
		manager.registerCommand(new GetSignpostCount());
		manager.registerCommand(new SetWaystoneCount());
		manager.registerCommand(new SetSignpostCount());
		manager.registerCommand(new DiscoverWaystone());
		manager.registerCommand(new ListAllWaystones());
	}
}