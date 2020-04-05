package gollorum.signpost.blocks;

import gollorum.signpost.BlockHandler;
import gollorum.signpost.SPEventHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.event.UseSignpostEvent;
import gollorum.signpost.items.CalibratedPostWrench;
import gollorum.signpost.items.PostBrush;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.network.messages.RequestTextureMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Paintable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

public abstract class SuperPostPost extends BlockContainer {
	
	protected SuperPostPost(Material p_i45386_1_) {super(p_i45386_1_);}

	@Override
	public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
		SuperPostPostTile superTile = getSuperTile(world, x, y, z);
		if (world.isRemote || !canUse((EntityPlayerMP) player, superTile)) {
			return;
		}
		Object hit = getHitTarget(world, x, y, z, player);
		if(isHitWaystone(hit)&&player.isSneaking()){
			superTile.destroyWaystone();
			
		}else if (player.getHeldItem() != null){
			Item item = player.getHeldItem().getItem();
			if(item instanceof PostWrench) {
				if (player.isSneaking()) {
					if(preShiftClick(hit, superTile, player, x, y, z)){
						shiftClickWrench(hit, superTile, player, x, y, z);
					}
				} else {
					clickWrench(hit, superTile, player, x, y, z);
				}
			}else if(item instanceof CalibratedPostWrench) {
				if (player.isSneaking()) {
					if(preShiftClick(hit, superTile, player, x, y, z)){
						shiftClickCalibratedWrench(hit, superTile, player, x, y, z);
					}
				} else {
					clickCalibratedWrench(hit, superTile, player, x, y, z);
				}
			}else if(item instanceof PostBrush) {
				clickBrush(hit, superTile, player, x, y, z);
			}else{
				if (player.isSneaking()) {
					if(preShiftClick(hit, superTile, player, x, y, z)){
						shiftClick(hit, superTile, player, x, y, z);
					}
				}else{
					click(hit, superTile, player, x, y, z);
				}
			}
		}else{
			if (player.isSneaking()) {
				if(preShiftClick(hit, superTile, player, x, y, z)){
					shiftClickBare(hit, superTile, player, x, y, z);
				}
			}else{
				clickBare(hit, superTile, player, x, y, z);
			}
		}
		sendPostBasesToAll(superTile);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer playerIn, int facing, float hitX, float hitY, float hitZ) {
		if(MinecraftForge.EVENT_BUS.post(new UseSignpostEvent(playerIn, world, x, y, z)) || world.isRemote){
			return true;
		}
		Object hit = getHitTarget(world, x, y, z, playerIn);
		SuperPostPostTile superTile = getSuperTile(world, x, y, z);
		EntityPlayerMP player = (EntityPlayerMP) playerIn;
		if(isHitWaystone(hit)){
			rightClickWaystone(superTile, player, x, y, z);
		}else if (player.getHeldItem() != null){
			if(player.getHeldItem().getItem() instanceof PostWrench){
				if(!canUse(player, superTile)){
					return true;
				}
				rightClickWrench(hit, superTile, player, x, y, z);
				sendPostBasesToAll(superTile);
			}else if(player.getHeldItem().getItem() instanceof CalibratedPostWrench){
				if(!canUse(player, superTile)){
					return true;
				}
				rightClickCalibratedWrench(hit, superTile, player, x, y, z);
			}else if(player.getHeldItem().getItem() instanceof PostBrush){
				if(!canUse(player, superTile)){
					return true;
				}
				rightClickBrush(hit, superTile, player, x, y, z);
				sendPostBasesToAll(superTile);
			}else if(superTile.isAwaitingPaint()){
				if(superTile.getPaintObject()==null){
					superTile.setAwaitingPaint(false);
				}else{
					if(!canUse(player, superTile)){
						return true;
					}
					NetworkHandler.netWrap.sendTo(new RequestTextureMessage(x, y, z), (EntityPlayerMP)player);
				}
			}else if(Block.getBlockFromItem(player.getHeldItem().getItem()) instanceof BasePost){
				if(rightClickBase(superTile, (EntityPlayerMP) player, x, y, z)){
					preRightClick(hit, superTile, player, x, y, z);
				}
			}else{
				preRightClick(hit, superTile, player, x, y, z);
			}
		} else {
			preRightClick(hit, superTile, player, x, y, z);
		}
		return true;
	}
	
	protected boolean canUse(EntityPlayerMP player, SuperPostPostTile tile) {
		return ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canUse(player, ""+tile.owner);
	}

	private void rightClickWaystone(SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BaseInfo ws = superTile.getBaseInfo();
		if(!player.isSneaking()){
			if(!PostHandler.doesPlayerKnowNativeWaystone((EntityPlayerMP) player, ws)){
				if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
					NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.discovered", "<Waystone>", ws.getName()), (EntityPlayerMP) player);
				}
				PostHandler.addDiscovered(player.getUniqueID(), ws);
			}
		}else{
			if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()
					&& ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canUse((EntityPlayerMP) player, ""+ws.owner)) {
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, x, y, z), (EntityPlayerMP) player);
			}
		}
	}

	/**
	 * @return whether the signpost already is a waystone
	 */
	private boolean rightClickBase(SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		if(superTile.isWaystone()){
			return true;
		}
		if(!(ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canUse((EntityPlayerMP) player, ""+superTile.owner) && SPEventHandler.INSTANCE.checkWaystoneCount(player))){
			return true;
		}
		MyBlockPos blockPos = superTile.toPos();
		MyBlockPos telePos = new MyBlockPos(player);
		String name = BasePost.generateName();
		UUID owner = player.getUniqueID();
		BaseInfo ws = new BaseInfo(name, blockPos, telePos, owner);
		PostHandler.addWaystone(ws);
		PostHandler.addDiscovered(owner, ws);
		NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.PLACED,superTile.getWorldObj(), telePos.x, telePos.y, telePos.z, name));
		NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, x, y, z), player);
		superTile.isWaystone = true;
		player.inventory.consumeInventoryItem(Item.getItemFromBlock(BlockHandler.base));
		return false;
	}

	private void preRightClick(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z){
		if(isHitWaystone(hitObj)){
			BaseInfo ws = superTile.getBaseInfo();
			if(!PostHandler.doesPlayerKnowNativeWaystone((EntityPlayerMP) player, ws)){
				if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
	NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.discovered", "<Waystone>", ws.getName()), (EntityPlayerMP) player);
				}
				PostHandler.addDiscovered(player.getUniqueID(), ws);
			}
		}else{
			rightClick(hitObj, superTile, player, x, y, z);
		}
	}

	private boolean preShiftClick(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		if(isHitWaystone(hitObj)){
			superTile.destroyWaystone();
			return false;
		}else{
			return true;
		}
	}

	protected abstract boolean isHitWaystone(Object hitObj);

	public abstract void clickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void rightClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void shiftClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);

	public abstract void clickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void rightClickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void shiftClickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);

	public abstract void clickBrush(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void rightClickBrush(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	
	public abstract void click(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void rightClick(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void shiftClick(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	
	public abstract void clickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void shiftClickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);

	public abstract void sendPostBasesToAll(SuperPostPostTile superTile);
	public abstract void sendPostBasesToServer(SuperPostPostTile superTile);
	
	public static SuperPostPostTile getSuperTile(World world, int x, int y, int z){
		return (SuperPostPostTile) world.getTileEntity(x, y, z);
	}
	
	public static SuperPostPostTile getSuperTile(MyBlockPos pos){
		TileEntity tile = pos.getTile();
		if(tile == null){
			pos.getWorld().getBlock(pos.x, pos.y, pos.z);
			tile = pos.getTile();
		}
		if(tile instanceof SuperPostPostTile){
			return (SuperPostPostTile) tile;
		}
		return null;
	}
	
	public static void updateServer(MyBlockPos pos){
		SuperPostPostTile tile = getSuperTile(pos);
		if(tile == null){
			return;
		}
		tile.getSuperBlock().sendPostBasesToAll(tile);
	}
	
	public static void updateClient(MyBlockPos pos){
		SuperPostPostTile tile = getSuperTile(pos);
		if(tile == null){
			return;
		}
		tile.getSuperBlock().sendPostBasesToServer(tile);
	}

	public abstract Object getHitTarget(World world, int x, int y, int z, EntityPlayer/*MP*/ player);

	public abstract Paintable getPaintableByHit(SuperPostPostTile tile, Object hit);

	public int getRenderType() {
		return -1;
	}

	public boolean renderAsNormalBlock() {
		return false;
	}

	public boolean isOpaqueCube() {
		return false;
	}

	public static void placeClient(World world, MyBlockPos blockPos, EntityPlayer player) {
		getSuperTile(world, blockPos.x, blockPos.y, blockPos.z).owner = player.getUniqueID();
	}

	public static void placeServer(World world, MyBlockPos blockPos, EntityPlayerMP player) {
		getSuperTile(world, blockPos.x, blockPos.y, blockPos.z).owner = player.getUniqueID();
	}

}
