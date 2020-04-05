package gollorum.signpost;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkRegistry;
import gollorum.signpost.commands.*;
import gollorum.signpost.gui.SignGuiHandler;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.management.PostHandler.TeleportInformation;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.StonedHashSet;
import gollorum.signpost.util.collections.Lurchpaerchensauna;
import gollorum.signpost.worldGen.villages.NameLibrary;
import gollorum.signpost.worldGen.villages.VillageLibrary;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.UUID;

@Mod(modid = Signpost.MODID, version = Signpost.VERSION, name = "SignPost")
public class Signpost{

	@Instance
	public static Signpost instance;
	public static final String MODID = "signpost";
	public static final String VERSION = "1.08.4";

	public static final int GuiBaseID = 0;
	public static final int GuiPostID = 1;
	public static final int GuiBigPostID = 2;
	public static final int GuiPostBrushID = 3;
	public static final int GuiPostRotationID = 4;
	
	public static NBTTagCompound saveFile;
	public static final Logger LOG = LogManager.getLogger(MODID);

	public static File configFile;
	public static File configFolder;
	
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
        
		proxy.init();
		
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new SignGuiHandler());
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event){
		ConfigHandler.postInit();
		PostHandler.setNativeWaystones(new StonedHashSet());
		PostHandler.setPosts(new Lurchpaerchensauna<MyBlockPos, DoubleBaseInfo>());
		PostHandler.setBigPosts(new Lurchpaerchensauna<MyBlockPos, BigBaseInfo>());
		PostHandler.awaiting = new Lurchpaerchensauna<UUID, TeleportInformation>();
	}

    @EventHandler
    public void preServerStart(FMLServerAboutToStartEvent event) {
        PostHandler.init();
        VillageLibrary.init();
    }

    @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent e){
    	PostHandler.init();
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