package gollorum.signpost.management;

import gollorum.signpost.Signpost;
import gollorum.signpost.management.ConfigHandler.RecipeCost;
import gollorum.signpost.management.ConfigHandler.SecurityLevel;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class ClientConfigStorage {
	
	public static ClientConfigStorage INSTANCE = new ClientConfigStorage();
	
	private boolean skipTeleportConfirm;
	
	private boolean deactivateTeleportation;
	private boolean interdimensional;
	private int maxWaystones;
	private int maxSignposts;
	private int maxDist;
	private Item cost;
	private String paymentItem;
	private int costMult;
	private int costBase;

	private RecipeCost signRec;
	private RecipeCost waysRec;

	private SecurityLevel securityLevelWaystone;
	private SecurityLevel securityLevelSignpost;
	private boolean disableDiscovery;
    
	private boolean disableVillageGeneration; 
	private int villageWaystonesWeight;
	private int villageMaxSignposts;
	private int villageSignpostsWeight;
	private boolean onlyVillageTargets;

	private String[] allowedCraftingModels;
	private String[] allowedVillageModels;

	public void postInit(){
		cost = (Item) Item.REGISTRY.getObject(new ResourceLocation(paymentItem));
		if(cost==null){
			cost = (Item) Item.REGISTRY.getObject(new ResourceLocation("minecraft:"+paymentItem));
		}
	}
	
	public boolean skipTeleportConfirm() {
		return skipTeleportConfirm;
	}
	public void setSkipTeleportConfirm(boolean skipTeleportConfirm) {
		this.skipTeleportConfirm = skipTeleportConfirm;
	}

	public boolean deactivateTeleportation() {
		return deactivateTeleportation;
	}
	public void setDeactivateTeleportation(boolean deactivateTeleportation) {
		this.deactivateTeleportation = deactivateTeleportation;
	}

	public boolean interdimensional() {
		return interdimensional;
	}
	public void setInterdimensional(boolean interdimensional) {
		this.interdimensional = interdimensional;
	}

	public int getMaxWaystones() {
		return maxWaystones;
	}
	public void setMaxWaystones(int maxWaystones) {
		this.maxWaystones = maxWaystones;
	}

	public int getMaxSignposts() {
		return maxSignposts;
	}
	public void setMaxSignposts(int maxSignposts) {
		this.maxSignposts = maxSignposts;
	}

	public int getMaxDist() {
		return maxDist;
	}
	public void setMaxDist(int maxDist) {
		this.maxDist = maxDist;
	}

	public Item getCost() {
		return cost;
	}
	public void setCost(Item cost) {
		this.cost = cost;
	}

	public String getPaymentItem() {
		return paymentItem;
	}
	public void setPaymentItem(String paymentItem) {
		this.paymentItem = paymentItem;
	}

	public int getCostMult() {
		return costMult;
	}
	public void setCostMult(int costMult) {
		this.costMult = costMult;
	}

	public void setCostBase(int costBase) {
		this.costBase = costBase;
	}
	public int getCostBase() {
		return costBase;
	}

	public RecipeCost getSignRec() {
		return signRec;
	}
	public void setSignRec(RecipeCost signRec) {
		this.signRec = signRec;
	}

	public RecipeCost getWaysRec() {
		return waysRec;
	}
	public void setWaysRec(RecipeCost waysRec) {
		this.waysRec = waysRec;
	}

	public SecurityLevel getSecurityLevelWaystone() {
		return securityLevelWaystone;
	}
	public void setSecurityLevelWaystone(SecurityLevel securityLevelWaystone) {
		this.securityLevelWaystone = securityLevelWaystone;
	}

	public SecurityLevel getSecurityLevelSignpost() {
		return securityLevelSignpost;
	}
	public void setSecurityLevelSignpost(SecurityLevel securityLevelSignpost) {
		this.securityLevelSignpost = securityLevelSignpost;
	}

	public boolean isDisableDiscovery() {
		return disableDiscovery;
	}
	public void setDisableDiscovery(boolean disableDiscovery) {
		this.disableDiscovery = disableDiscovery;
	}
	 
	public boolean isDisableVillageGeneration() {
		return disableVillageGeneration;
	}
	public void setDisableVillageGeneration(boolean disableVillageGeneration) {
		this.disableVillageGeneration = disableVillageGeneration;
	}

	public int getVillageWaystonesWeight() {
		return villageWaystonesWeight;
	}
	public void setVillageWaystonesWeight(int villageWaystonesWeight) {
		this.villageWaystonesWeight = villageWaystonesWeight;
	}

	public int getVillageSignpostsWeight() {
		return villageSignpostsWeight;
	}
	public void setVillageSignpostsWeight(int villageSignpostsWeight) {
		this.villageSignpostsWeight = villageSignpostsWeight;
	}

	public int getVillageMaxSignposts() {
		return villageMaxSignposts;
	}
	public void setVillageMaxSignposts(int villageMaxSignposts) {
		this.villageMaxSignposts = villageMaxSignposts;
	}

	public boolean isOnlyVillageTargets() {
		return onlyVillageTargets;
	}
	public void setOnlyVillageTargets(boolean onlyVillageTargets) {
		this.onlyVillageTargets = onlyVillageTargets;
	}

	public String[] getAllowedCraftingModels() {
		return allowedCraftingModels;
	}
	public void setAllowedCraftingModels(String[] allowedCraftingModels) {
		this.allowedCraftingModels = allowedCraftingModels;
	}

	public String[] getAllowedVillageModels() {
		return allowedVillageModels;
	}
	public void setAllowedVillageModels(String[] allowedVillageModels) {
		this.allowedVillageModels = allowedVillageModels;
	}

	private ClientConfigStorage(){}

}
