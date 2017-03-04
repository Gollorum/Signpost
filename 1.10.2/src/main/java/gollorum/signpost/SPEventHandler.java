package gollorum.signpost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.PlayerProvider;
import gollorum.signpost.management.PlayerStore;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.management.WorldSigns;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

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
	
	/*@SubscribeEvent
	public void loggedOut(PlayerLoggedOutEvent event){
	}*/

	// ServerSide
	@SubscribeEvent
	public void loggedIn(PlayerLoggedInEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			NetworkHandler.netWrap.sendTo(new InitPlayerResponseMessage(), (EntityPlayerMP) event.player);
			NetworkHandler.netWrap.sendTo(new SendAllPostBasesMessage(), (EntityPlayerMP) event.player);
			PlayerStore store = event.player.getCapability(PlayerProvider.STORE_CAP, null);
			if(store.known==null){
				store.init((EntityPlayerMP) event.player);
			}else{
				PostHandler.addAllDiscoveredByName(event.player.getUniqueID(), store.known);
				store.init((EntityPlayerMP) event.player);
			}
		}
	}

	public static final ResourceLocation PLAYER_CAP = new ResourceLocation(Signpost.MODID, "playerstore");
	 
	@SubscribeEvent
	public void attachCapability(AttachCapabilitiesEvent.Entity event) {
		if (event.getEntity() instanceof EntityPlayerMP) {
			event.addCapability(PLAYER_CAP, new PlayerProvider());
		}
	}
	
	@SubscribeEvent
	public void onLoad(WorldEvent.Load event) {
		if(!event.getWorld().isRemote) {
			WorldSigns.worldSigns(event.getWorld());
		}
	}

	@SubscribeEvent
	public void onSave(WorldEvent.Save event) {
		if(!event.getWorld().isRemote) {
			WorldSigns.worldSigns(event.getWorld());
		}
	}
	
	@SubscribeEvent
	public void onBlockBreak(BreakEvent event){
		TileEntity tile = event.getWorld().getTileEntity(event.getPos());
		if(tile instanceof PostPostTile){
			if(event.getPlayer().getHeldItemMainhand()!=null&&event.getPlayer().getHeldItemMainhand().getItem() instanceof PostWrench){
				event.setCanceled(true);
			}else{
				if(!FMLCommonHandler.instance().getSide().equals(Side.SERVER)){
					PostPost.onBlockDestroy(new MyBlockPos(event.getWorld(), event.getPos(), event.getPlayer().dimension));
				}
			}
		}else{
			if(tile instanceof BasePostTile){
				if(!FMLCommonHandler.instance().getSide().equals(Side.SERVER)){
					BasePost.onBlockDestroy(new MyBlockPos(event.getWorld(), event.getPos(), event.getPlayer().dimension));
				}
			}
		}
	}
}
