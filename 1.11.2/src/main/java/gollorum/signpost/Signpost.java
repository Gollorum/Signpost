package gollorum.signpost;

import java.io.File;
import java.util.UUID;

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
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.StonedHashSet;
import gollorum.signpost.util.collections.Lurchpaerchensauna;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = Signpost.MODID, version = Signpost.VERSION, name = "SignPost")
public class Signpost{

	@Instance
	public static Signpost instance;
	public static final String MODID = "signpost";
	public static final String VERSION = "1.06";

	public static final int GuiBaseID = 0;
	public static final int GuiPostID = 1;
	public static final int GuiBigPostID = 2;
	public static final int GuiPostBrushID = 3;
	public static final int GuiPostRotationID = 4;

	public static File configFile;
	
	public static NBTTagCompound saveFile;
	
	@SidedProxy(clientSide = "gollorum.signpost.ClientProxy", serverSide = "gollorum.signpost.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preinit(FMLPreInitializationEvent event) {

		File configFolder = new File(event.getModConfigurationDirectory() + "/" + MODID);
		configFolder.mkdirs();
		configFile = new File(configFolder.getPath(), MODID + ".cfg");
		ConfigHandler.init(configFile);
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
		PostHandler.allWaystones = new StonedHashSet();
		PostHandler.posts = new Lurchpaerchensauna<MyBlockPos, DoubleBaseInfo>();
		PostHandler.bigPosts = new Lurchpaerchensauna<MyBlockPos, BigBaseInfo>();
		PostHandler.awaiting = new Lurchpaerchensauna<UUID, TeleportInformation>();
	}
	
	@EventHandler
	public void serverAboutToStart(FMLServerAboutToStartEvent e){
		PostHandler.init();
	}
    
	@EventHandler
	public void serverStarting(FMLServerStartingEvent e) {
		ServerCommandManager manager = (ServerCommandManager) e.getServer().getCommandManager();
		manager.registerCommand(new ConfirmTeleportCommand());
		manager.registerCommand(new GetWaystoneCount());
		manager.registerCommand(new GetSignpostCount());
		manager.registerCommand(new SetWaystoneCount());
		manager.registerCommand(new SetSignpostCount());
		ConfigHandler.init(configFile);
	}
}