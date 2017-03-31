package gollorum.signpost.management;

import java.io.File;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class ConfigHandler {

	private static Configuration config;

	public static boolean skipTeleportConfirm;
	
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
		loadClientSettings();
		loadLimitation();
		loadSecurity();
		config.save();
	}
	
	public static void postInit(){
		cost = (Item) Item.REGISTRY.getObject(new ResourceLocation(paymentItem));
		if(cost==null){
			cost = (Item) Item.REGISTRY.getObject(new ResourceLocation("minecraft:"+paymentItem));
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

	public static boolean isOp(EntityPlayerMP player){
		return player.canUseCommand(2, "");
	}
	
	public static boolean isCreative(EntityPlayerMP player){
		return player.capabilities.isCreativeMode;
	}
	
	public static String costName(){
		return I18n.translateToLocal(cost.getUnlocalizedName()+".name");
	}
}
