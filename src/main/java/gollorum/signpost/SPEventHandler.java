package gollorum.signpost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gollorum.signpost.management.PlayerStore;
import gollorum.signpost.management.WorldSigns;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.WorldEvent;

public class SPEventHandler {

	private static HashMap<Runnable, Integer> tasks = new HashMap<Runnable, Integer>();

	/**
	 * Schedules a task
	 * 
	 * @param task
	 *            The task to execute
	 * @param delay
	 *            The delay in ticks (1s/20)
	 */
	public static void scheduleTask(Runnable task, int delay) {
		tasks.put(task, delay);
	}

	@SubscribeEvent
	public void onTick(TickEvent event) {
		if (!(event instanceof TickEvent.ServerTickEvent || event instanceof TickEvent.ClientTickEvent)) {
			return;
		}
		// time++;
		HashSet<Runnable> deletedTasks = new HashSet<Runnable>();
		for (Entry<Runnable, Integer> now : tasks.entrySet()) {
			if ((now.setValue(now.getValue() - 1)) < 2) {
				now.getKey().run();
				deletedTasks.add(now.getKey());
			}
		}
		for (Runnable now : deletedTasks) {
			tasks.remove(now);
		}
	}

	// ServerSide
	@SubscribeEvent
	public void loggedIn(PlayerLoggedInEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			NetworkHandler.netWrap.sendTo(new InitPlayerResponseMessage(), (EntityPlayerMP) event.player);
			NetworkHandler.netWrap.sendTo(new SendAllPostBasesMessage(), (EntityPlayerMP) event.player);
			
		}
	}

	@SubscribeEvent
	public void entConst(EntityEvent.EntityConstructing event) {
		if (event.entity instanceof EntityPlayerMP) {
			event.entity.registerExtendedProperties("KnownWaystones", new PlayerStore());
		}
	}
	
	@SubscribeEvent
	public void onLoad(WorldEvent.Load event) {
		if(!event.world.isRemote) {
			WorldSigns.worldSigns(event.world);
		}
	}

	@SubscribeEvent
	public void onSave(WorldEvent.Save event) {
		if(!event.world.isRemote) {
			WorldSigns.worldSigns(event.world);
		}
	}
}
