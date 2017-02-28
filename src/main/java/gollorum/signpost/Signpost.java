package gollorum.signpost;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.gui.SignGuiHandler;
import net.minecraft.nbt.NBTTagCompound;

@Mod(modid = Signpost.MODID, version = Signpost.VERSION, name = "SignPost")
public class Signpost{

	@Instance
	public static Signpost instance;
	public static final String MODID = "signpost";
	public static final String VERSION = "1.0";

	public static final int GuiBaseID = 0;
	public static final int GuiPostID = 1;
	
	public static boolean serverSide;
	
	public static NBTTagCompound saveFile;
	
	@SidedProxy(clientSide = "gollorum.signpost.ClientProxy", serverSide = "gollorum.signpost.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preinit(FMLPreInitializationEvent event) {
		
		serverSide = FMLCommonHandler.instance().getSide().equals(Side.SERVER);

		proxy.init();
		
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new SignGuiHandler());
	}
	
}