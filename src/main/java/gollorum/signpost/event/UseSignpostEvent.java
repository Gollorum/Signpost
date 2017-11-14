package gollorum.signpost.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired server- and client-side upon right-clicking a signpost.
 */
public class UseSignpostEvent extends Event {

	public EntityPlayer player;
	public World world;
	public int x;
	public int y;
	public int z;
	
	public UseSignpostEvent(EntityPlayer player, World world, int x, int y, int z) {
		super();
		this.player = player;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
    public boolean isCancelable(){
        return true;
    }
	
}
