package gollorum.signpost.minecraft.config;

public interface ITeleportConfig {
	boolean enableTeleport();
	int maximumDistance();
	boolean enforceDiscovery();
	boolean enableAcrossDimensions();
	boolean allowVehicle();
	boolean allowLead();

	String costItem();
	int constantPayment();
	int distancePerPayment();
}
