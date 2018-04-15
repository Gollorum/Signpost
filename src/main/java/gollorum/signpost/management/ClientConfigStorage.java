package gollorum.signpost.management;

import gollorum.signpost.Signpost;
import gollorum.signpost.management.ConfigHandler.RecipeCost;
import gollorum.signpost.management.ConfigHandler.SecurityLevel;
import net.minecraft.item.Item;

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

	private RecipeCost signRec;
	private RecipeCost waysRec;

	private SecurityLevel securityLevelWaystone;
	private SecurityLevel securityLevelSignpost;
	private boolean disableDiscovery;

	private float villageWaystonePropability;
	private int villageMinSignposts;
	private int villageMaxSignposts;
	private boolean onlyVillageTargets;
	
	public void postInit(){
		cost = (Item) Item.itemRegistry.getObject(paymentItem);
		if(cost==null){
			cost = (Item) Item.itemRegistry.getObject("minecraft:"+paymentItem);
		}
		Signpost.proxy.blockHandler.registerRecipes();
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

	public float getVillageWaystonePropability() {
		return villageWaystonePropability;
	}

	public void setVillageWaystonePropability(float villagePropability) {
		this.villageWaystonePropability = villagePropability;
	}

	public int getVillageMinSignposts() {
		return villageMinSignposts;
	}

	public void setVillageMinSignposts(int villageMinSignposts) {
		this.villageMinSignposts = villageMinSignposts;
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

	private ClientConfigStorage(){}

}
