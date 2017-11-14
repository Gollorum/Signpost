package gollorum.signpost.event;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Server-side only
 */
public class UpdateWaystoneEvent extends Event{

	public static enum WaystoneEventType{
		PLACED, NAMECHANGED, DESTROYED;
	}
	
	public WaystoneEventType type;
	public World world;
	public int x, y, z;
	public String name;
	
	public UpdateWaystoneEvent(WaystoneEventType type, World world, int x, int y, int z, String name) {
		super();
		this.type = type;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.name = name;
	}
	
}
