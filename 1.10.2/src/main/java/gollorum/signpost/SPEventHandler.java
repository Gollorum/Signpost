package gollorum.signpost;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.PostPost;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PlayerProvider;
import gollorum.signpost.management.PlayerStore;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.management.WorldSigns;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SPEventHandler {

	private static HashMap<Runnable, Integer> tasks = new HashMap<Runnable, Integer>();
	private static HashSet<BoolRun> predicatedTasks = new HashSet<BoolRun>();

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

	public static void scheduleTask(BoolRun task){
		predicatedTasks.add(task);
	}

	public static boolean cancelTask(BoolRun task){
		return predicatedTasks.remove(task);
	}
	
	@SubscribeEvent
	public void onTick(TickEvent event) {
		if (!(event instanceof TickEvent.ServerTickEvent || event instanceof TickEvent.ClientTickEvent)) {
			return;
		}
		// time++;
		HashMap<Runnable, Integer> remainingTasks = new HashMap<Runnable, Integer>();
		for (Entry<Runnable, Integer> now : tasks.entrySet()) {
			int val = now.getValue()-1;
			if (val < 2) {
				now.getKey().run();
			}else{
				remainingTasks.put(now.getKey(), val);
			}
		}
		tasks = remainingTasks;
		
		HashSet<BoolRun> remainingPreds = new HashSet<BoolRun>();
		for(BoolRun now: predicatedTasks){
			if(!now.run()){
				remainingPreds.add(now);
			}
		}
		predicatedTasks = remainingPreds;
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
	public void oBlockPlace(PlaceEvent event){
		if(!(event.getPlayer() instanceof EntityPlayerMP)){
			if(event.getState().getBlock() instanceof BasePost){
				BasePost.placeClient(event.getWorld(), new MyBlockPos("", event.getPos(), event.getPlayer().dimension), event.getPlayer());
			}else if(event.getState().getBlock() instanceof PostPost){
				PostPost.placeClient(event.getWorld(), new MyBlockPos("", event.getPos(), event.getPlayer().dimension), event.getPlayer());
			}
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
		if(event.getState().getBlock() instanceof BasePost){
			if(!ConfigHandler.securityLevelWaystone.canUse(player)){
				BasePost.getWaystoneRootTile(event.getWorld(), event.getPos()).onBlockDestroy(new MyBlockPos(event.getWorld(), event.getPos(), player.dimension));
				event.setCanceled(true);
			}else{
				BasePost.placeServer(event.getWorld(), new MyBlockPos(event.getWorld(), event.getPos(), event.getPlayer().dimension), (EntityPlayerMP) event.getPlayer());
			}
		}else if(event.getState().getBlock() instanceof PostPost){
			if(!ConfigHandler.securityLevelSignpost.canUse(player)){
				PostPost.getWaystonePostTile(event.getWorld(), event.getPos()).onBlockDestroy(new MyBlockPos(event.getWorld(), event.getPos(), player.dimension));
				event.setCanceled(true);
			}else{
				PostPost.placeServer(event.getWorld(), new MyBlockPos(event.getWorld(), event.getPos(), event.getPlayer().dimension), (EntityPlayerMP) event.getPlayer());
			}
			
		}
	}

	@SubscribeEvent
	public void onBlockBreak(BreakEvent event){
		TileEntity tile = event.getWorld().getTileEntity(event.getPos());
		if(tile instanceof PostPostTile && event.getPlayer().getHeldItemMainhand()!=null && event.getPlayer().getHeldItemMainhand().getItem() instanceof PostWrench){
				event.setCanceled(true);
		}
		if(!(event.getPlayer() instanceof EntityPlayerMP)){
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
		if(event.getState().getBlock() instanceof BasePost){
			if(!ConfigHandler.securityLevelWaystone.canUse(player)){
				event.setCanceled(true);
			}else{
				BasePost.getWaystoneRootTile(event.getWorld(), event.getPos()).onBlockDestroy(new MyBlockPos(event.getWorld(), event.getPos(), player.dimension));
			}
		}else if(event.getState().getBlock() instanceof PostPost){
			if(!ConfigHandler.securityLevelSignpost.canUse(player)){
				event.setCanceled(true);
			}else{
				PostPost.getWaystonePostTile(event.getWorld(), event.getPos()).onBlockDestroy(new MyBlockPos(event.getWorld(), event.getPos(), player.dimension));
			}
		}
	}
}
