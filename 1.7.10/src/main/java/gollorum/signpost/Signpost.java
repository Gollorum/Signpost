package gollorum.signpost;

import java.io.File;
import java.util.UUID;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import gollorum.signpost.commands.ConfirmTeleportCommand;
import gollorum.signpost.commands.GetSignpostCount;
import gollorum.signpost.commands.GetWaystoneCount;
import gollorum.signpost.commands.SetSignpostCount;
import gollorum.signpost.commands.SetWaystoneCount;
import gollorum.signpost.gui.SignGuiHandler;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.management.PostHandler.TeleportInformation;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.StonedHashSet;
import gollorum.signpost.util.collections.Lurchpaerchensauna;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.nbt.NBTTagCompound;

@Mod(modid = Signpost.MODID, version = Signpost.VERSION, name = "SignPost")
public class Signpost{

	@Instance
	public static Signpost instance;
	public static final String MODID = "signpost";
	public static final String VERSION = "1.05.4";

	public static final int GuiBaseID = 0;
	public static final int GuiPostID = 1;
	public static final int GuiBigPostID = 2;
	public static final int GuiPostBrushID = 3;
	public static final int GuiPostRotationID = 4;
	
	public static NBTTagCompound saveFile;
	
	public static File configFile;
	
	@SidedProxy(clientSide = "gollorum.signpost.ClientProxy", serverSide = "gollorum.signpost.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preinit(FMLPreInitializationEvent event) {
		
		File configFolder = new File(event.getModConfigurationDirectory() + "/" + MODID);
		configFolder.mkdirs();
		ConfigHandler.init(new File(configFolder.getPath(), MODID + ".cfg"));
        
		proxy.init();
		
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new SignGuiHandler());
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event){
		ConfigHandler.postInit();
		PostHandler.allWaystones = new StonedHashSet();
		PostHandler.posts = new Lurchpaerchensauna<MyBlockPos, DoubleBaseInfo>();
		PostHandler.bigPosts = new Lurchpaerchensauna<MyBlockPos, BigBaseInfo>();
		PostHandler.awaiting = new Lurchpaerchensauna<UUID, TeleportInformation>();
	}

    @EventHandler
    public void preServerStart(FMLServerAboutToStartEvent event) {
        PostHandler.init();
    }
    
	@EventHandler
	public void registerCommands(FMLServerStartingEvent e) {
		ServerCommandManager manager = (ServerCommandManager) e.getServer().getCommandManager();
		manager.registerCommand(new ConfirmTeleportCommand());
		manager.registerCommand(new GetWaystoneCount());
		manager.registerCommand(new GetSignpostCount());
		manager.registerCommand(new SetWaystoneCount());
		manager.registerCommand(new SetSignpostCount());
	}

}