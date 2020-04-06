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
import gollorum.signpost.management.ConfigHandler;
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
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

public abstract class SuperPostPost extends BlockContainer {

	protected SuperPostPost(Material materialIn) {super(materialIn);}

	@Override
	public void onBlockClicked(World world, BlockPos pos, EntityPlayer playerIn) {
		SuperPostPostTile superTile = getSuperTile(world, pos);
		if (world.isRemote || !canUse((EntityPlayerMP) playerIn, superTile)) {
			return;
		}
		EntityPlayerMP player = (EntityPlayerMP) playerIn;
		Object hit = getHitTarget(world, pos.getX(), pos.getY(), pos.getZ(), player);
		if(isHitWaystone(hit)&&player.isSneaking()){
			superTile.destroyWaystone();
		}
		else if (!PostHandler.isHandEmpty(player)){
			Item item = player.getHeldItemMainhand().getItem();
			if(item instanceof PostWrench) {
				if (player.isSneaking()) {
					if(preShiftClick(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ())){
						shiftClickWrench(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
					}
				} else {
					clickWrench(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
				}
			}else if(item instanceof CalibratedPostWrench) {
				if (player.isSneaking()) {
					if(preShiftClick(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ())){
						shiftClickCalibratedWrench(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
					}
				} else {
					clickCalibratedWrench(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
				}
			}else if(item instanceof PostBrush) {
				clickBrush(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
			}else{
				if (player.isSneaking()) {
					if(preShiftClick(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ())){
						shiftClick(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
					}
				}else{
					click(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
				}
			}
		}else{
			if (player.isSneaking()) {
				if(preShiftClick(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ())){
					shiftClickBare(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
				}
			}else{
				clickBare(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
			}
		}
		sendPostBasesToAll(superTile);
	}
	
	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(MinecraftForge.EVENT_BUS.post(new UseSignpostEvent(playerIn, worldIn, pos.getX(), pos.getY(), pos.getZ())) || worldIn.isRemote){
			return true;
		}
		
		Object hit = getHitTarget(worldIn, pos.getX(), pos.getY(), pos.getZ(), playerIn);
		SuperPostPostTile superTile = getSuperTile(worldIn, pos);
		EntityPlayerMP player = (EntityPlayerMP) playerIn;
		if(isHitWaystone(hit)){
			rightClickWaystone(superTile, player, pos.getX(), pos.getY(), pos.getZ());
		}
		else if (!PostHandler.isHandEmpty(playerIn)){
			if(playerIn.getHeldItemMainhand().getItem() instanceof PostWrench){
				if(!canUse(player, superTile)){
					return true;
				}
				rightClickWrench(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
				sendPostBasesToAll(superTile);
			}else if(playerIn.getHeldItemMainhand().getItem() instanceof CalibratedPostWrench){
				if(!canUse(player, superTile)){
					return true;
				}
				rightClickCalibratedWrench(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
			}else if(playerIn.getHeldItemMainhand().getItem() instanceof PostBrush){
				if(!canUse(player, superTile)){
					return true;
				}
				rightClickBrush(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
				sendPostBasesToAll(superTile);
			}else if(superTile.isAwaitingPaint()){
				if(!canUse(player, superTile)){
					return true;
				}
				if(superTile.getPaintObject()==null){
					superTile.setAwaitingPaint(false);
				}else{
					NetworkHandler.netWrap.sendTo(new RequestTextureMessage(pos.getX(), pos.getY(), pos.getZ(), hand, facing, hitX, hitY, hitZ), (EntityPlayerMP)playerIn);
				}
			}else if(Block.getBlockFromItem(playerIn.getHeldItemMainhand().getItem()) instanceof BasePost){
				if(rightClickBase(superTile, (EntityPlayerMP) playerIn, pos)){
					preRightClick(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
				}
			}else{
				preRightClick(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
			}
		} else {
			preRightClick(hit, superTile, player, pos.getX(), pos.getY(), pos.getZ());
		}
		return true;
	}
	
	protected boolean canUse(EntityPlayerMP player, SuperPostPostTile tile) {
		return ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canUse(player, ""+tile.owner);
	}

	private void rightClickWaystone(SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BaseInfo ws = superTile.getBaseInfo();
		if(!player.isSneaking()){
			if(!PostHandler.doesPlayerKnowNativeWaystone(player, ws)){
				if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
					NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.discovered", "<Waystone>", ws.getName()), player);
				}
				PostHandler.addDiscovered(player.getUniqueID(), ws);
			}
		}else{
			if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()
					&& ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canUse(player, ""+ws.owner)) {
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, x, y, z), player);
			}
		}
	}

	/**
	 * @return whether the signpost already is a waystone
	 */
	private boolean rightClickBase(SuperPostPostTile superTile, EntityPlayerMP player, BlockPos pos) {
		if(superTile.isWaystone()){
			return true;
		}
		if(!(ClientConfigStorage.INSTANCE.getSecurityLevelSignpost().canUse(player, ""+superTile.owner) && SPEventHandler.INSTANCE.checkWaystoneCount(player))){
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
		MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.PLACED,superTile.getWorld(), telePos.x, telePos.y, telePos.z, name));
		NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, pos.getX(), pos.getY(), pos.getZ()), player);
		superTile.isWaystone = true;
		if(!ConfigHandler.isCreative(player)){
			player.inventory.clearMatchingItems(Item.getItemFromBlock(BlockHandler.base), -1, 1, null);
		}
		return false;
	}
	
	private void preRightClick(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z){
		if(isHitWaystone(hitObj)){
			BaseInfo ws = superTile.getBaseInfo();
			if(!PostHandler.doesPlayerKnowNativeWaystone(player, ws)){
				if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
					NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.discovered", "<Waystone>", ws.getName()), player);
				}
				PostHandler.addDiscovered(player.getUniqueID(), ws);
			}
		}else{
			rightClick(hitObj, superTile, player, x, y, z);
		}
	}

	private boolean preShiftClick(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		if(isHitWaystone(hitObj)){
			superTile.destroyWaystone();
			return false;
		}else{
			return true;
		}
	}

	protected abstract boolean isHitWaystone(Object hitObj);

	public abstract void clickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	public abstract void rightClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	public abstract void shiftClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);

	public abstract void clickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	public abstract void rightClickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	public abstract void shiftClickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);

	public abstract void clickBrush(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	public abstract void rightClickBrush(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	
	public abstract void click(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	public abstract void rightClick(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	public abstract void shiftClick(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	
	public abstract void clickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);
	public abstract void shiftClickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z);

	public abstract void sendPostBasesToAll(SuperPostPostTile superTile);
	public abstract void sendPostBasesToServer(SuperPostPostTile superTile);
	
	public static SuperPostPostTile getSuperTile(World world, BlockPos pos){
		return (SuperPostPostTile) world.getTileEntity(pos);
	}
    
	public static SuperPostPostTile getSuperTile(MyBlockPos pos) {
		TileEntity tile = pos.getTile();
		if (tile == null) {
			pos.getWorld().getBlockState(pos.toBlockPos());
			tile = pos.getTile();
		}
		if (tile instanceof SuperPostPostTile) {
			return (SuperPostPostTile) tile;
		}
		return null;
	}

	public static void updateServer(MyBlockPos pos) {
		SuperPostPostTile tile = getSuperTile(pos);
		if (tile == null) {
			return;
		}
		tile.getSuperBlock().sendPostBasesToAll(tile);
	}

	public static void updateClient(MyBlockPos pos) {
		SuperPostPostTile tile = getSuperTile(pos);
		if (tile == null) {
			return;
		}
		tile.getSuperBlock().sendPostBasesToServer(tile);
	}

	public abstract Object getHitTarget(World world, int x, int y, int z, EntityPlayer/*MP*/ player);

	public abstract Paintable getPaintableByHit(SuperPostPostTile tile, Object hit);
	
	@Override
	public EnumBlockRenderType getRenderType(IBlockState state){
		return EnumBlockRenderType.INVISIBLE;
	}

	@Override
    public boolean isOpaqueCube(IBlockState state){
        return false;
    }
	
	@Override
	public boolean isFullCube(IBlockState state){
		return false;
	}

	@Override
	public BlockRenderLayer getRenderLayer(){
		return BlockRenderLayer.TRANSLUCENT;
	}

	public static void placeClient(World world, MyBlockPos blockPos, EntityPlayer player) {
		getSuperTile(world, blockPos.toBlockPos()).owner = player.getUniqueID();
	}

	public static void placeServer(World world, MyBlockPos blockPos, EntityPlayerMP player) {
		getSuperTile(world, blockPos.toBlockPos()).owner = player.getUniqueID();
	}

}