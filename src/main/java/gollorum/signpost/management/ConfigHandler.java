package gollorum.signpost.management;

import gollorum.signpost.blocks.BaseModelPost;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ConfigHandler {

	private static Configuration config;

	private static boolean skipTeleportConfirm;  
    
	private static boolean deactivateTeleportation;
	private static boolean interdimensional;
	private static int maxWaystones;
	private static int maxSignposts;
	private static int maxDist;
	private static Item cost;
	private static String paymentItem;
	private static int costMult;
	private static int costBase;

	private static RecipeCost signRec;
	private static RecipeCost waysRec;

	private static SecurityLevel securityLevelWaystone; 
	private static SecurityLevel securityLevelSignpost;
	private static boolean disableDiscovery;

	private static boolean disableVillageGeneration;
	private static int villageWaystonesWeight;
	private static int villageMaxSignposts;
	private static int villageSignpostsWeight;
	private static boolean onlyVillageTargets;

	private static String[] allowedCraftingModels;
	private static String[] allowedVillageModels;

	public enum SecurityLevel{
		ALL(true), 
		OWNERS(true), 
		CREATIVEONLY(false), 
		OPONLY(false);

		public final boolean canCraft;
		
		private SecurityLevel(boolean canCraft) {
			this.canCraft = canCraft;
		}
		
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
			return this.equals(ALL)||
				   this.equals(OWNERS)||
				   isOp(player)||
				   (isCreative(player) && this.equals(CREATIVEONLY));
		}
		public boolean canUse(EntityPlayerMP player, String owner){
			return this.equals(ALL)||
					   isOp(player)||
					   (this.equals(OWNERS) && owner.equals(player.getUniqueID().toString()))||
					   (isCreative(player) && this.equals(CREATIVEONLY));
		}
	}

	public enum RecipeCost{
		DEACTIVATED, NORMAL, EXPENSIVE, VERY_EXPENSIVE;
		public static String[] allValues(){
			String[] ret = new String[values().length];
			RecipeCost[] values = values();
			for (int i = 0; i < values.length; i++) {
				ret[i] = values[i].toString();
			}
			return ret;
		}
	}
	
	public static void init(File file) {
		config = new Configuration(file);
		loadClientSettings();
		loadLimitation();
		loadSecurity();
	    loadWorldGen();
		config.save();
	}
	
	public static void postInit(){
		cost = (Item) Item.REGISTRY.getObject(new ResourceLocation(paymentItem));
		if(cost==null){
			cost = (Item) Item.REGISTRY.getObject(new ResourceLocation("minecraft:"+paymentItem));
		}
		ClientConfigStorage.INSTANCE.setCost(cost);
	}
	   
	private static void loadWorldGen() {
		String category = "WorldGen";
		config.addCustomCategoryComment(category, "World generation settings");

		disableVillageGeneration = config.getBoolean("disableVillageGeneration", category, false,
				"Disables the generation of signposts and waystones in villages");

	    villageMaxSignposts = config.getInt("villageMaxSignposts", category, 1, 0, Integer.MAX_VALUE,
				"The maximum count of signposts spawning in villages");

	    villageSignpostsWeight = config.getInt("villageSignpostsWeight", category, 20, 0, Integer.MAX_VALUE,
				"Defines the village component weight of signposts");

	    villageWaystonesWeight = config.getInt("villageWaystoneWeight", category, 20, 0, Integer.MAX_VALUE,
				"Defines the village component weight of waystones");

	    onlyVillageTargets = config.getBoolean("onlyVillageDestinations", category, true,
				"Defines wether signposts in villages can be generated with waystones placed by players as destinations");

	    ClientConfigStorage.INSTANCE.setDisableVillageGeneration(disableVillageGeneration); 
	    ClientConfigStorage.INSTANCE.setVillageMaxSignposts(villageMaxSignposts); 
	    ClientConfigStorage.INSTANCE.setVillageSignpostsWeight(villageSignpostsWeight); 
	    ClientConfigStorage.INSTANCE.setVillageWaystonesWeight(villageWaystonesWeight);
		ClientConfigStorage.INSTANCE.setOnlyVillageTargets(onlyVillageTargets);
	}

	public static void loadClientSettings(){
		String category = "Client Settings";
		
		config.addCustomCategoryComment(category, "Client-Side settings");
		
		skipTeleportConfirm = config.getBoolean("skipTeleportConfirm", category, true,
				"Directly teleports the player on waystone right-click");

		ClientConfigStorage.INSTANCE.setSkipTeleportConfirm(skipTeleportConfirm);
	}
	
	public static void loadLimitation(){
		String category = "Limitaion";
		
		config.addCustomCategoryComment(category, "Teleport limitaion settings");
		
		deactivateTeleportation = config.getBoolean("deactivateTeleportation", category, false, 
				"Deactivates teleportation and the waystone recipe, since it isn't needed");
		
		interdimensional = config.getBoolean("interdimensional", category, true, 
				"Enables interdimensional teleportation (e.g. overworld-nether)");

		maxWaystones = config.getInt("maxWaystones", category, -1, -1, Integer.MAX_VALUE, 
				"The amount of waystones a player is allowed to place (-1 = unlimited)");

		maxSignposts = config.getInt("maxSignposts", category, -1, -1, Integer.MAX_VALUE, 
				"The amount of signposts a player is allowed to place (-1 = unlimited)");
		
		maxDist = config.getInt("maxDistance", category, -1, -1, (int)Math.sqrt(Integer.MAX_VALUE), 
				"The allowed distance between signpost an waystone (-1 = unlimited)");
		
		paymentItem = config.getString("paymentItem", category, "", 
				"The item players have to pay in order to use a signpost (e.g. minecraft:ender_pearl, '' = free)");
		
		costMult = config.getInt("distancePerPayment", category, 0, 0, Integer.MAX_VALUE, 
				"The distance a Player can teleport with one item (the total cost of a teleportation is calculated using the total distance)(0 = unlimited)");
		
		costBase = config.getInt("constantPaymentPerTeleport", category, 1, 1, Integer.MAX_VALUE, 
				"The amount of items players always have to pay when teleporting, regardless of the distance. For the total cost, this amount will be added to the distance-based cost.");
	
		signRec = RecipeCost.valueOf(config.getString("signpostRecipeCost", category, "NORMAL", 
				"Changes the recipe for signposts (NORMAL/EXPENSIVE/VERY_EXPENSIVE/DEACTIVATED)", RecipeCost.allValues()));

		waysRec = RecipeCost.valueOf(config.getString("waystoneRecipeCost", category, "NORMAL", 
				"Changes the recipe for waystones (NORMAL/EXPENSIVE/VERY_EXPENSIVE/DEACTIVATED)", RecipeCost.allValues()));

		allowedCraftingModels = config.getStringList("waystoneModelCraftingTypes", category, BaseModelPost.allTypeNames,
				"Decide what waystone models can be crafted. You can look up the model names at https://www.curseforge.com/minecraft/mc-mods/signpost/pages/waystone-models", BaseModelPost.allTypeNames);

		allowedVillageModels = config.getStringList("waystoneModelVillageTypes", category, BaseModelPost.allDefaultVillageTypeNames,
				"Decide what waystone models are generated in villages. You can look up the model names at https://www.curseforge.com/minecraft/mc-mods/signpost/pages/waystone-models", BaseModelPost.allTypeNames);

		ClientConfigStorage.INSTANCE.setDeactivateTeleportation(deactivateTeleportation);
		ClientConfigStorage.INSTANCE.setInterdimensional(interdimensional);
		ClientConfigStorage.INSTANCE.setMaxWaystones(maxWaystones);
		ClientConfigStorage.INSTANCE.setMaxSignposts(maxSignposts);
		ClientConfigStorage.INSTANCE.setMaxDist(maxDist);
		ClientConfigStorage.INSTANCE.setPaymentItem(paymentItem);
		ClientConfigStorage.INSTANCE.setCostMult(costMult);
		ClientConfigStorage.INSTANCE.setCostBase(costBase);
		ClientConfigStorage.INSTANCE.setSignRec(signRec);
		ClientConfigStorage.INSTANCE.setWaysRec(waysRec);
		ClientConfigStorage.INSTANCE.setAllowedCraftingModels(allowedCraftingModels);
		ClientConfigStorage.INSTANCE.setAllowedVillageModels(allowedVillageModels);
	}
	
	public static void loadSecurity(){
		String category = "Security";
		
		config.addCustomCategoryComment(category, "Security settings");

		securityLevelWaystone = SecurityLevel.valueOf(config.getString("waystonePermissionLevel", category, "ALL",
				"Defines which players can place and edit a waystone (ALL, OWNERS, CREATIVEONLY, OPONLY). OPs are always included, 'OWNERS' = everyone can place, only the owner+OPs can edit.", SecurityLevel.allValues()));

		securityLevelSignpost = SecurityLevel.valueOf(config.getString("signpostPermissionLevel", category, "ALL",
				"Defines which players can place and edit a signpost (ALL, OWNERS, CREATIVEONLY, OPONLY). OPs are always included, 'OWNERS' = everyone can place, only the owner+OPs can edit.", SecurityLevel.allValues()));
	
		disableDiscovery = config.getBoolean("disableDiscovery", category, false,
				"Allows players to travel to waystones without the need to discover them");
	
		ClientConfigStorage.INSTANCE.setSecurityLevelWaystone(securityLevelWaystone);
		ClientConfigStorage.INSTANCE.setSecurityLevelSignpost(securityLevelSignpost);
		ClientConfigStorage.INSTANCE.setDisableDiscovery(disableDiscovery);
	}

	public static boolean isOp(EntityPlayerMP player){
		return player.canUseCommand(2, "");
	}
	
	public static boolean isCreative(EntityPlayer player){
		return player.capabilities.isCreativeMode;
	}
	
	public static String costName(){
		return I18n.format(ClientConfigStorage.INSTANCE.getCost().getTranslationKey()+".name");
	}
    
	@Deprecated
	public static boolean isSkipTeleportConfirm() {
		return skipTeleportConfirm;
	}

	@Deprecated
	public static boolean isDeactivateTeleportation() {
		return deactivateTeleportation;
	}

	@Deprecated
	public static boolean isInterdimensional() {
		return interdimensional;
	}

	@Deprecated
	public static int getMaxDist() {
		return maxDist;
	}

	@Deprecated
	public static String getPaymentItem() {
		return paymentItem;
	}

	@Deprecated
	public static int getCostMult() {
		return costMult;
	}

	@Deprecated
	public static int getCostBase() {
		return costBase;
	}

	@Deprecated
	public static RecipeCost getSignRec() {
		return signRec;
	}

	@Deprecated
	public static RecipeCost getWaysRec() {
		return waysRec;
	}

	@Deprecated
	public static SecurityLevel getSecurityLevelWaystone() {
		return securityLevelWaystone;
	}

	@Deprecated
	public static SecurityLevel getSecurityLevelSignpost() {
		return securityLevelSignpost;
	}

	@Deprecated
	public static boolean isDisableVillageGeneration() { 
	    return disableVillageGeneration;
	}

	@Deprecated
	public static int getVillageMaxSignposts() {
		return villageMaxSignposts;
	}

	@Deprecated
	public static int getVillageSignpostsWeight() {
		return villageSignpostsWeight;
	}

	@Deprecated
	public static int getVillageWaystonesWeight() {
		return villageWaystonesWeight;
	}

	@Deprecated
	public static boolean isOnlyVillageTargets() {
		return onlyVillageTargets;
	}

	@Deprecated
	public static String[] getAllowedCraftingModels() {
		return allowedCraftingModels;
	}

	@Deprecated
	public static String[] getAllowedVillageModels() {
		return allowedVillageModels;
	}
  
}
