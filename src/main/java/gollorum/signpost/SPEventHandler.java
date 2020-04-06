package gollorum.signpost;

import gollorum.signpost.blocks.BaseModelPost;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.WaystoneContainer;
import gollorum.signpost.blocks.tiles.BasePostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.items.CalibratedPostWrench;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.*;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.collections.CollectionUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

public class SPEventHandler {

	private static Map<Runnable, Integer> serverTasks = new HashMap<Runnable, Integer>();
	private static Collection<BoolRun> serverPredicatedTasks = new ArrayList<BoolRun>();
	
	private static Map<Runnable, Integer> clientTasks = new HashMap<Runnable, Integer>();
	private static Collection<BoolRun> clientPredicatedTasks = new ArrayList<BoolRun>();

	public static final SPEventHandler INSTANCE = new SPEventHandler();
	private SPEventHandler(){}

	/**
	 * Schedules a task
	 * 
	 * @param task
	 *            The task to execute
	 * @param delay
	 *            The delay in ticks (1s/20)
	 */
	public static void scheduleTask(Runnable task, int delay) {
		if(FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER)){
			serverTasks.put(task, delay);
		}else{
			clientTasks.put(task, delay);
		}
	}

	public static void scheduleTask(BoolRun task){
		if(FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER)){
			serverPredicatedTasks.add(task);
		}else{
			clientPredicatedTasks.add(task);
		}
	}

	public static boolean cancelTask(BoolRun task){
		if(FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER)){
			return serverPredicatedTasks.remove(task);
		}else{
			return clientPredicatedTasks.remove(task);
		}
	}

	@SubscribeEvent
	public void onServerTick(TickEvent event) {
		if (!(event instanceof TickEvent.ServerTickEvent)) {
			return;
		}
		Map<Runnable, Integer> oldTasks = serverTasks;
		serverTasks = new HashMap<>();
		serverTasks.putAll(CollectionUtils.mutateOr(
				oldTasks,
				(task, delay) -> delay > 1, // condition
				(task, delay) -> delay - 1, // mutation
				(task, delay) -> task.run() // elseAction
		));
		Collection<BoolRun> oldPredicateTasks = serverPredicatedTasks;
		serverPredicatedTasks = new HashSet<>();
		serverPredicatedTasks.addAll(CollectionUtils.where(oldPredicateTasks, boolRun -> !boolRun.run()));
	}

	@SubscribeEvent
	public void onClientTick(TickEvent event) {
		if (!(event instanceof TickEvent.ClientTickEvent)) {
			return;
		}
		Map<Runnable, Integer> oldTasks = clientTasks;
		clientTasks = new HashMap<>();
		clientTasks.putAll(CollectionUtils.mutateOr(
				oldTasks,
				(task, delay) -> delay > 1, // condition
				(task, delay) -> delay - 1, // mutation
				(task, delay) -> task.run() // elseAction
		));
		Collection<BoolRun> oldPredicateTasks = clientPredicatedTasks;
		clientPredicatedTasks = new HashSet<>();
		clientPredicatedTasks.addAll(CollectionUtils.where(oldPredicateTasks, boolRun -> !boolRun.run()));
	}
	
	// ServerSide
	@SubscribeEvent
	public void loggedIn(PlayerLoggedInEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			NetworkHandler.netWrap.sendTo(new InitPlayerResponseMessage(), (EntityPlayerMP) event.player);
			NetworkHandler.netWrap.sendTo(new SendAllPostBasesMessage(), (EntityPlayerMP) event.player);
			NetworkHandler.netWrap.sendTo(new SendAllBigPostBasesMessage(), (EntityPlayerMP) event.player);
			PlayerStore store = event.player.getCapability(PlayerProvider.STORE_CAP, null);
			store.init((EntityPlayerMP) event.player);
		}
	}

	public static final ResourceLocation PLAYER_CAP = new ResourceLocation(Signpost.MODID, "playerstore");
	 
	@SubscribeEvent
	public void attachCapability(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayerMP) {
			PlayerProvider provider = new PlayerProvider((EntityPlayerMP) event.getObject());
			event.addCapability(PLAYER_CAP, provider);
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
		MyBlockPos blockPos = new MyBlockPos(event.getPos(), event.getPlayer().dimension);
		if(!(event.getPlayer() instanceof EntityPlayerMP)){
			if(event.getState().getBlock() instanceof BasePost){
				BasePost.placeClient(event.getWorld(), blockPos, event.getPlayer());
			}else if(event.getState().getBlock() instanceof BaseModelPost){
				BaseModelPost.placeClient(event.getWorld(), blockPos, event.getPlayer());
			}else if(event.getState().getBlock() instanceof SuperPostPost){
				SuperPostPost.placeClient(event.getWorld(), blockPos, event.getPlayer());
			}
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
		MyBlockPos eventPos = new MyBlockPos(event.getPos(), player.dimension);
		if(event.getState().getBlock() instanceof BasePost){
			BasePostTile tile = BasePost.getWaystoneRootTile(event.getWorld(), event.getPos());
			if(!(ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canPlace(player) && checkWaystoneCount(player))){
				tile.onBlockDestroy(eventPos);
				event.setCanceled(true);
			}else{
				BasePost.placeServer(event.getWorld(), eventPos, (EntityPlayerMP) event.getPlayer());
			}
		}else if(event.getState().getBlock() instanceof BaseModelPost){
			BasePostTile tile = BaseModelPost.getWaystoneRootTile(event.getWorld(), event.getPos());
			if(!(ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canPlace(player) && checkWaystoneCount(player))){
				tile.onBlockDestroy(eventPos);
				event.setCanceled(true);
			}else{
				BaseModelPost.placeServer(event.getWorld(), eventPos, (EntityPlayerMP) event.getPlayer());
			}
		}else if(event.getState().getBlock() instanceof SuperPostPost){
			SuperPostPostTile tile = SuperPostPost.getSuperTile(event.getWorld(), event.getPos());
			if(!(ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canPlace(player) && checkSignpostCount(player))){
				tile.onBlockDestroy(eventPos);
				event.setCanceled(true);
			}else{
				SuperPostPost.placeServer(event.getWorld(), eventPos, (EntityPlayerMP) event.getPlayer());
			}
		}
	}

	public boolean checkWaystoneCount(EntityPlayerMP player){
		PlayerRestrictions restrictions = PostHandler.getPlayerKnownWaystonePositions(player.getUniqueID());
		int remaining = restrictions.remainingWaystones;
		if(remaining == 0){
			player.sendMessage(new TextComponentString("You are not allowed to place more waystones"));
			return false;
		}
		if(remaining > 0) restrictions.remainingWaystones--;
		return true;
	}
	
	public void updateWaystoneCount(WaystoneContainer tile){
		if(tile == null || tile.getBaseInfo() == null){
			return;
		}
		UUID owner = tile.getBaseInfo().owner;
		if(owner == null){
			return;
		}
		PlayerRestrictions restrictions = PostHandler.getPlayerKnownWaystonePositions(owner);
		if(restrictions.remainingWaystones >= 0){
			restrictions.remainingWaystones++;
		}
	}

	private boolean checkSignpostCount(EntityPlayerMP player){
		PlayerRestrictions restrictions = PostHandler.getPlayerKnownWaystonePositions(player.getUniqueID());
		int remaining = restrictions.remainingSignposts;
		if(remaining == 0){
			player.sendMessage(new TextComponentString("You are not allowed to place more signposts"));
			return false;
		}
		if(remaining > 0) restrictions.remainingSignposts--;
		return true;
	}
	
	private void updateSignpostCount(SuperPostPostTile tile){
		if(tile == null || tile.owner == null){
			return;
		}
		PlayerRestrictions restrictions = PostHandler.getPlayerKnownWaystonePositions(tile.owner);
		if(restrictions.remainingSignposts >= 0){
			restrictions.remainingSignposts++;
		}
	}

	@SubscribeEvent
	public void onBlockBreak(BreakEvent event){
		try{
			TileEntity tile = event.getWorld().getTileEntity(event.getPos());
			if(tile instanceof SuperPostPostTile 
					&& !PostHandler.isHandEmpty(event.getPlayer()) 
					&& (event.getPlayer().getHeldItemMainhand().getItem() instanceof PostWrench 
							|| event.getPlayer().getHeldItemMainhand().getItem() instanceof CalibratedPostWrench
							|| event.getPlayer().getHeldItemMainhand().getItem().equals(Items.WHEAT_SEEDS)
							|| event.getPlayer().getHeldItemMainhand().getItem().equals(Items.SNOWBALL)
							|| event.getPlayer().getHeldItemMainhand().getItem().equals(Item.getItemFromBlock(Blocks.VINE)))){
				event.setCanceled(true);
				((SuperPostPost)tile.getBlockType()).onBlockClicked(event.getWorld(), event.getPos(), event.getPlayer());
				return;
			}
			if(!(event.getPlayer() instanceof EntityPlayerMP)){
				return;
			}
			EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
			if(event.getState().getBlock() instanceof BasePost){
				BasePostTile t = BasePost.getWaystoneRootTile(event.getWorld(), event.getPos());
				if(!ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canUse(player, ""+t.getBaseInfo().owner)){
					event.setCanceled(true);
				}else{
					updateWaystoneCount(t);
					t.onBlockDestroy(new MyBlockPos(event.getPos(), player.dimension));
				}
			}else if(event.getState().getBlock() instanceof BaseModelPost){
				BasePostTile t = BaseModelPost.getWaystoneRootTile(event.getWorld(), event.getPos());
				if(!ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canUse(player, ""+t.getBaseInfo().owner)){
					event.setCanceled(true);
				}else{
					updateWaystoneCount(t);
					t.onBlockDestroy(new MyBlockPos(event.getPos(), player.dimension));
				}
			}else if(event.getState().getBlock() instanceof SuperPostPost){
				SuperPostPostTile t = SuperPostPost.getSuperTile(event.getWorld(), event.getPos());
				if(!ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canUse(player, ""+t.owner)){
					event.setCanceled(true);
				}else{
					updateSignpostCount(t);
					t.onBlockDestroy(new MyBlockPos(event.getPos(), player.dimension));
				}
			}
		}catch(Exception e){}
	}
}
