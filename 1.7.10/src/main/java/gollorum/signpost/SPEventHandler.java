package gollorum.signpost;

import java.util.Map.Entry;
import java.util.UUID;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.SuperPostPostTile;
import gollorum.signpost.items.CalibratedPostWrench;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PlayerStore;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.management.WorldSigns;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.InitPlayerResponseMessage;
import gollorum.signpost.network.messages.SendAllBigPostBasesMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.StringSet;
import gollorum.signpost.util.collections.Lurchpaerchensauna;
import gollorum.signpost.util.collections.Lurchsauna;
import gollorum.signpost.util.collections.Pair;
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

public class SPEventHandler {

	private static Lurchpaerchensauna<Runnable, Integer> tasks = new Lurchpaerchensauna<Runnable, Integer>();
	private static Lurchsauna<BoolRun> predicatedTasks = new Lurchsauna<BoolRun>();

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
		Lurchpaerchensauna<Runnable, Integer> remainingTasks = new Lurchpaerchensauna<Runnable, Integer>();
		for (Entry<Runnable, Integer> now : tasks.entrySet()) {
			int val = now.getValue()-1;
			if (val < 2) {
				now.getKey().run();
			}else{
				remainingTasks.put(now.getKey(), val);
			}
		}
		tasks = remainingTasks;
		
		Lurchsauna<BoolRun> remainingPreds = new Lurchsauna<BoolRun>();
		for(BoolRun now: predicatedTasks){int a = 0;
			if(!now.run()){
				remainingPreds.add(now);
			}
		}
		predicatedTasks = remainingPreds;
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
		if(!(event.player instanceof EntityPlayerMP)){
			if(event.block instanceof BasePost){
				BasePost.placeClient(event.world, new MyBlockPos("", event.x, event.y, event.z, event.player.dimension), event.player);
			}else if(event.block instanceof SuperPostPost){
				SuperPostPost.placeClient(event.world, new MyBlockPos("", event.x, event.y, event.z, event.player.dimension), event.player);
			}
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP)event.player;
		if(event.block instanceof BasePost){
			BasePostTile tile = BasePost.getWaystoneRootTile(event.world, event.x, event.y, event.z);
			if(!(ConfigHandler.securityLevelWaystone.canPlace(player) && checkWaystoneCount(player))){
				tile.onBlockDestroy(new MyBlockPos(event.world, event.x, event.y, event.z, player.dimension));
				event.setCanceled(true);
			}else{
				BasePost.placeServer(event.world, new MyBlockPos(event.world.getWorldInfo().getWorldName(), event.x, event.y, event.z, event.player.dimension), (EntityPlayerMP) event.player);
			}
		}else if(event.block instanceof SuperPostPost){
			SuperPostPostTile tile = SuperPostPost.getSuperTile(event.world, event.x, event.y, event.z);
			if(!(ConfigHandler.securityLevelSignpost.canPlace(player) && checkSignpostCount(player))){
				tile.onBlockDestroy(new MyBlockPos(event.world, event.x, event.y, event.z, player.dimension));
				event.setCanceled(true);
			}else{
				SuperPostPost.placeServer(event.world, new MyBlockPos(event.world.getWorldInfo().getWorldName(), event.x, event.y, event.z, event.player.dimension), (EntityPlayerMP) event.player);
			}
		}
	}

	private boolean checkWaystoneCount(EntityPlayerMP player){
		Pair<StringSet, Pair<Integer, Integer>> pair = PostHandler.playerKnownWaystones.get(player.getUniqueID());
		int remaining = pair.b.a;
		if(remaining == 0){
			player.addChatMessage(new ChatComponentText("You are not allowed to place more waystones"));
			return false;
		}else{
			if(remaining > 0){
				pair.b.a--;
			}
			return true;
		}
	}
	
	private void updateWaystoneCount(BasePostTile tile){
		if(tile == null || tile.getBaseInfo() == null){
			return;
		}
		UUID owner = tile.getBaseInfo().owner;
		if(owner == null){
			return;
		}
		Pair<StringSet, Pair<Integer, Integer>> pair = PostHandler.playerKnownWaystones.get(owner);
		if(pair!=null && pair.b.a>=0){
			pair.b.a++;
		}
	}

	private boolean checkSignpostCount(EntityPlayerMP player){
		Pair<StringSet, Pair<Integer, Integer>> pair = PostHandler.playerKnownWaystones.get(player.getUniqueID());
		int remaining = pair.b.b;
		if(remaining == 0){
			player.addChatMessage(new ChatComponentText("You are not allowed to place more signposts"));
			return false;
		}else{
			if(remaining > 0){
				pair.b.b--;
			}
			return true;
		}
	}
	
	private void updateSignpostCount(SuperPostPostTile tile){
		if(tile == null || tile.owner == null){
			return;
		}
		Pair<StringSet, Pair<Integer, Integer>> pair = PostHandler.playerKnownWaystones.get(tile.owner);
		if(pair.b.b>=0){
			pair.b.b++;
		}
	}

	@SubscribeEvent
	public void onBlockBreak(BreakEvent event){
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
			if(!ConfigHandler.securityLevelWaystone.canUse(player, ""+t.getBaseInfo().owner)){
				event.setCanceled(true);
			}else{
				updateWaystoneCount(t);
				t.onBlockDestroy(new MyBlockPos(event.world, event.x, event.y, event.z, player.dimension));
			}
		}else if(event.block instanceof SuperPostPost){
			SuperPostPostTile t = SuperPostPost.getSuperTile(event.world, event.x, event.y, event.z);
			if(!ConfigHandler.securityLevelSignpost.canUse(player, ""+t.owner)){
				event.setCanceled(true);
			}else{
				updateSignpostCount(t);
				t.onBlockDestroy(new MyBlockPos(event.world, event.x, event.y, event.z, player.dimension));
			}
		}
	}
}
