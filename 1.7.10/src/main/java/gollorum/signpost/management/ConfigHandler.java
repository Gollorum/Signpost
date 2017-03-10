package gollorum.signpost.management;

import java.io.File;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;

public class ConfigHandler {

	private static Configuration config;
	
	public static boolean deactivateTeleportation;
	public static boolean interdimensional;
	public static int maxDist;
	public static Item cost;
	public static String paymentItem;
	public static int costMult;

	public static SecurityLevel securityLevelWaystone;
	public static SecurityLevel securityLevelSignpost;
	
	public enum SecurityLevel{
		ALL, CREATIVEONLY, OPONLY;
		public static String[] allValues(){
			String[] ret = {
				ALL.toString(),
				CREATIVEONLY.toString(),
				OPONLY.toString()
			};
			return ret;
		}
		public boolean canUse(EntityPlayerMP player){
			return this.equals(ConfigHandler.SecurityLevel.ALL)||
					isOp(player)||
					(isCreative(player)&&this.equals(ConfigHandler.SecurityLevel.CREATIVEONLY));
		}
	}
	
	public static void init(File file) {
		config = new Configuration(file);
		loadLimitation();
		loadSecurity();
		config.save();
	}
	
	public static void postInit(){
		cost = (Item) Item.itemRegistry.getObject(paymentItem);
		if(cost==null){
			cost = (Item) Item.itemRegistry.getObject("minecraft:"+paymentItem);
		}
	}
	
	public static void loadLimitation(){
		String category = "Limitaion";
		
		config.addCustomCategoryComment(category, "Teleport limitaion settings");
		
		deactivateTeleportation = config.getBoolean("deactivateTeleportation", category, false, "Deactivates teleportation and the waystone recipe, since it isn't needed");
		
		interdimensional = config.getBoolean("interdimensional", category, true, "Enables interdimensional teleportation (e.g. overworld-nether)");
		
		maxDist = config.getInt("maxDistance", category, -1, -1, (int)Math.sqrt(Integer.MAX_VALUE), "The allowed distance between signpost an waystone (-1 = unlimited)");
		
		paymentItem = config.getString("paymentItem", category, "", "The item players have to pay in order to use a signpost (e.g. Minecraft:enderPearl, '' = free)");
		
		costMult = config.getInt("distancePerPayment", category, 0, 0, Integer.MAX_VALUE, "The distance a Player can teleport with one item (the total cost of a teleportation is calculated using the total distance)(0 = unlimited)");
	}
	
	public static void loadSecurity(){
		String category = "Security";
		
		config.addCustomCategoryComment(category, "Security settings");

		securityLevelWaystone = SecurityLevel.valueOf(config.getString("waystonePermissionLevel", category, "ALL", "Defines which players can place and edit a waystone (ALL, CREATIVEONLY, OPONLY)", SecurityLevel.allValues()));

		securityLevelSignpost = SecurityLevel.valueOf(config.getString("signpostPermissionLevel", category, "ALL", "Defines which players can place and edit a signpost (ALL, CREATIVEONLY, OPONLY)", SecurityLevel.allValues()));
	}

	public static boolean isOp(EntityPlayer player){
		return MinecraftServer.getServer().getConfigurationManager().func_152596_g(player.getGameProfile());

	}
	
	public static boolean isCreative(EntityPlayerMP player){
		return player.theItemInWorldManager.isCreative();
	}
	
	public static String costName(){
		String out = LanguageRegistry.instance().getStringLocalization(cost.getUnlocalizedName()+".name");
		if(out.equals("")){
			out = LanguageRegistry.instance().getStringLocalization(cost.getUnlocalizedName()+".name", "en_US");
		}
		return out;
	}
}
