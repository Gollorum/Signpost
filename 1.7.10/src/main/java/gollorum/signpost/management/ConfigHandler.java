package gollorum.signpost.management;

import java.io.File;
import java.util.UUID;

import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;

public class ConfigHandler {

	private static Configuration config;
	
	public static boolean skipTeleportConfirm;
	
	public static boolean deactivateTeleportation;
	public static boolean interdimensional;
	public static int maxWaystones;
	public static int maxSignposts;
	public static int maxDist;
	public static Item cost;
	public static String paymentItem;
	public static int costMult;

	public static SecurityLevel securityLevelWaystone;
	public static SecurityLevel securityLevelSignpost;
	
	public enum SecurityLevel{
		ALL, OWNERS, CREATIVEONLY, OPONLY;
		public static String[] allValues(){
			String[] ret = {
				ALL.toString(),
				OWNERS.toString(),
				CREATIVEONLY.toString(),
				OPONLY.toString()
			};
			return ret;
		}
		public boolean canPlace(EntityPlayerMP player){
			return this.equals(ConfigHandler.SecurityLevel.ALL)||
				   isOp(player)||
				   (isCreative(player)&&this.equals(ConfigHandler.SecurityLevel.CREATIVEONLY));
		}
		public boolean canUse(EntityPlayerMP player, UUID owner){
			return this.equals(ConfigHandler.SecurityLevel.ALL)||
				   isOp(player)||
				   (owner.toString().equals(player.getUniqueID().toString())&&this.equals(ConfigHandler.SecurityLevel.OWNERS))||
				   (isCreative(player)&&this.equals(ConfigHandler.SecurityLevel.CREATIVEONLY));
		}
	}
	
	public static void init(File file) {
		config = new Configuration(file);
		loadClientSettings();
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
	
	public static void loadClientSettings(){
		String category = "Client Settings";
		
		config.addCustomCategoryComment(category, "Client-Side settings");
		
		skipTeleportConfirm = config.getBoolean("skipTeleportConfirm", category, true, "Directly teleports the player on waystone right-click");
	}
	
	public static void loadLimitation(){
		String category = "Limitaion";
		
		config.addCustomCategoryComment(category, "Teleport limitaion settings");
		
		deactivateTeleportation = config.getBoolean("deactivateTeleportation", category, false, "Deactivates teleportation and the waystone recipe, since it isn't needed");
		
		interdimensional = config.getBoolean("interdimensional", category, true, "Enables interdimensional teleportation (e.g. overworld-nether)");

		maxWaystones = config.getInt("maxWaystones", category, -1, -1, Integer.MAX_VALUE, "The amount of waystones a player is allowed to place (-1 = unlimited)");

		maxSignposts = config.getInt("maxSignposts", category, -1, -1, Integer.MAX_VALUE, "The amount of signposts a player is allowed to place (-1 = unlimited)");
		
		maxDist = config.getInt("maxDistance", category, -1, -1, (int)Math.sqrt(Integer.MAX_VALUE), "The allowed distance between signpost an waystone (-1 = unlimited)");
		
		paymentItem = config.getString("paymentItem", category, "", "The item players have to pay in order to use a signpost (e.g. minecraft:enderPearl, '' = free)");
		
		costMult = config.getInt("distancePerPayment", category, 0, 0, Integer.MAX_VALUE, "The distance a Player can teleport with one item (the total cost of a teleportation is calculated using the total distance)(0 = unlimited)");
	}
	
	public static void loadSecurity(){
		String category = "Security";
		
		config.addCustomCategoryComment(category, "Security settings");

		securityLevelWaystone = SecurityLevel.valueOf(config.getString("waystonePermissionLevel", category, "ALL", "Defines which players can place and edit a waystone (ALL, OWNERS, CREATIVEONLY, OPONLY). OPs are always included, 'OWNERS' = everyone can place, only the owner+OPs can edit.", SecurityLevel.allValues()));

		securityLevelSignpost = SecurityLevel.valueOf(config.getString("signpostPermissionLevel", category, "ALL", "Defines which players can place and edit a signpost (ALL, OWNERS, CREATIVEONLY, OPONLY). OPs are always included, 'OWNERS' = everyone can place, only the owner+OPs can edit.", SecurityLevel.allValues()));
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
