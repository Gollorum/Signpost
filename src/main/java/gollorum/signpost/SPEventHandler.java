package gollorum.signpost;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.util.*;

public class SPEventHandler {

	private static Map<Runnable, Integer> serverTasks = new HashMap<>();
	private static Collection<BoolRun> serverPredicatedTasks = new ArrayList<>();

	private static Map<Runnable, Integer> clientTasks = new HashMap<>();
	private static Collection<BoolRun> clientPredicatedTasks = new ArrayList<>();
	
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

	/**
	 * return true when done
	 */
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
	
	@SubscribeEvent
	public void oBlockPlace(PlaceEvent event){
		MyBlockPos blockPos = new MyBlockPos(event.x, event.y, event.z, event.player.dimension);
		if(!(event.player instanceof EntityPlayerMP)){
			if(event.block instanceof BasePost){
				BasePost.placeClient(event.world, blockPos, event.player);
			}else if(event.block instanceof BaseModelPost){
				BaseModelPost.placeClient(event.world, blockPos, event.player);
			}else if(event.block instanceof SuperPostPost){
				SuperPostPost.placeClient(event.world, blockPos, event.player);
			}
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP)event.player;
		if(event.block instanceof BasePost){
			BasePostTile tile = BasePost.getWaystoneRootTile(event.world, event.x, event.y, event.z);
			if(!(ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canPlace(player) && checkWaystoneCount(player))){
				tile.onBlockDestroy(blockPos);
				event.setCanceled(true);
			}else{
				BasePost.placeServer(event.world, blockPos, (EntityPlayerMP) event.player);
			}
		}else if(event.block instanceof BaseModelPost){
			BasePostTile tile = BaseModelPost.getWaystoneRootTile(event.world, event.x, event.y, event.z);
			if(!(ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canPlace(player) && checkWaystoneCount(player))){
				tile.onBlockDestroy(blockPos);
				event.setCanceled(true);
			}else{
				BaseModelPost.placeServer(event.world, blockPos, (EntityPlayerMP) event.player);
			}
		}else if(event.block instanceof SuperPostPost){
			SuperPostPostTile tile = SuperPostPost.getSuperTile(event.world, event.x, event.y, event.z);
			if(!(ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canPlace(player) && checkSignpostCount(player))){
				tile.onBlockDestroy(blockPos);
				event.setCanceled(true);
			}else{
				SuperPostPost.placeServer(event.world, blockPos, (EntityPlayerMP) event.player);
			}
		}
	}

	public boolean checkWaystoneCount(EntityPlayerMP player){
		PlayerRestrictions restrictions = PostHandler.getPlayerKnownWaystonePositions(player.getUniqueID());
		int remaining = restrictions.remainingWaystones;
		if(remaining == 0){
			player.addChatMessage(new ChatComponentText("You are not allowed to place more waystones"));
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
			player.addChatMessage(new ChatComponentText("You are not allowed to place more signposts"));
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
			TileEntity tile = event.world.getTileEntity(event.x, event.y, event.z);
			if(tile instanceof SuperPostPostTile 
					&& event.getPlayer().getHeldItem()!=null 
					&& (event.getPlayer().getHeldItem().getItem() instanceof PostWrench 
							|| event.getPlayer().getHeldItem().getItem() instanceof CalibratedPostWrench
							|| event.getPlayer().getHeldItem().getItem().equals(Items.wheat_seeds)
							|| event.getPlayer().getHeldItem().getItem().equals(Items.snowball)
							|| event.getPlayer().getHeldItem().getItem().equals(Item.getItemFromBlock(Blocks.vine)))){
				event.setCanceled(true);
				((SuperPostPost)tile.blockType).onBlockClicked(event.world, event.x, event.y, event.z, event.getPlayer());
				return;
			}
			if(!(event.getPlayer() instanceof EntityPlayerMP)){
				return;
			}
			EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
			if(event.block instanceof BasePost){
				BasePostTile t = BasePost.getWaystoneRootTile(event.world, event.x, event.y, event.z);
				if(!ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canUse(player, ""+t.getBaseInfo().owner)){
					event.setCanceled(true);
				}else{
					updateWaystoneCount(t);
					t.onBlockDestroy(new MyBlockPos(event.x, event.y, event.z, player.dimension));
				}
			}else if(event.block instanceof BaseModelPost){
				BasePostTile t = BaseModelPost.getWaystoneRootTile(event.world, event.x, event.y, event.z);
				if(!ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canUse(player, ""+t.getBaseInfo().owner)){
					event.setCanceled(true);
				}else{
					updateWaystoneCount(t);
					t.onBlockDestroy(new MyBlockPos(event.x, event.y, event.z, player.dimension));
				}
			}else if(event.block instanceof SuperPostPost){
				SuperPostPostTile t = SuperPostPost.getSuperTile(event.world, event.x, event.y, event.z);
				if(!ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canUse(player, ""+t.owner)){
					event.setCanceled(true);
				}else{
					updateSignpostCount(t);
					t.onBlockDestroy(new MyBlockPos(event.x, event.y, event.z, player.dimension));
				}
			}
		}catch(Exception e){}
	}
}
