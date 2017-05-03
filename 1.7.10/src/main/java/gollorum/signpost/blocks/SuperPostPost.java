package gollorum.signpost.blocks;

import gollorum.signpost.event.UseSignpostEvent;
import gollorum.signpost.items.PostBrush;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.util.BlockPos;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public abstract class SuperPostPost extends BlockContainer {
	
	protected SuperPostPost(Material p_i45386_1_) {super(p_i45386_1_);}

	@Override
	public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
		SuperPostPostTile superTile = getSuperTile(world, x, y, z);
		if (world.isRemote || !ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player, superTile.owner)) {
			return;
		}
		Object hit = getHitTarget(world, x, y, z, player);
		if (player.getHeldItem() != null){
			Item item = player.getHeldItem().getItem();
			if(item instanceof PostWrench) {
				if (player.isSneaking()) {
					shiftClickWrench(hit, superTile, player, x, y, z);
				} else {
					clickWrench(hit, superTile, player, x, y, z);
				}
			}else{
				if (player.isSneaking()) {
					shiftClick(hit, superTile, player, x, y, z);
				}else{
					click(hit, superTile, player, x, y, z);
				}
			}
		}else{
			if (player.isSneaking()) {
				shiftClickBare(hit, superTile, player, x, y, z);
			}else{
				clickBare(hit, superTile, player, x, y, z);
			}
		}
		sendPostBasesToAll(superTile);
	}

	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(MinecraftForge.EVENT_BUS.post(new UseSignpostEvent(player, world, x, y, z)) || world.isRemote){
			return true;
		}
		Object hit = getHitTarget(world, x, y, z, player);
		SuperPostPostTile superTile = getSuperTile(world, x, y, z);
		if (player.getHeldItem() != null){
			if(player.getHeldItem().getItem() instanceof PostWrench){
				if(!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player, superTile.owner)){
					return true;
				}
				rightClickWrench(hit, superTile, player, x, y, z);
				sendPostBasesToAll(superTile);
			}else if(player.getHeldItem().getItem() instanceof PostBrush){
				if(!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player, superTile.owner)){
					return true;
				}
				rightClickBrush(hit, superTile, player, x, y, z);
				sendPostBasesToAll(superTile);
			}else{
				rightClick(hit, superTile, player, x, y, z);
			}
		} else {
			rightClick(hit, superTile, player, x, y, z);
		}
		return true;
	}

	public abstract void clickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void rightClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	public abstract void shiftClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z);
	
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

	public abstract Object getHitTarget(World world, int x, int y, int z, EntityPlayer/*MP*/ player);

	public int getRenderType() {
		return -1;
	}

	public boolean renderAsNormalBlock() {
		return false;
	}

	public boolean isOpaqueCube() {
		return false;
	}

	public static void placeClient(World world, BlockPos blockPos, EntityPlayer player) {
		getSuperTile(world, blockPos.x, blockPos.y, blockPos.z).owner = player.getUniqueID();
	}

	public static void placeServer(World world, BlockPos blockPos, EntityPlayerMP player) {
		getSuperTile(world, blockPos.x, blockPos.y, blockPos.z).owner = player.getUniqueID();
	}

}
