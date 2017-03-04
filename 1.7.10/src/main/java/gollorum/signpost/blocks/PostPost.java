package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.network.messages.TeleportMeMessage;
import gollorum.signpost.util.BaseInfo;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class PostPost extends BlockContainer {

	public PostPost() {
		super(Material.wood);
		setBlockName("SignpostPost");
		setCreativeTab(CreativeTabs.tabTransport);
		setBlockTextureName("Minecraft:planks_oak");
		this.setHardness(2);
		this.setResistance(100000);
		float f = 15F / 32;
		this.setBlockBounds(0.5f - f, 0.0F, 0.5F - f, 0.5F + f, 1, 0.5F + f);
	}

	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
		PostPostTile tile = new PostPostTile();
		tile.isItem = false;
		return tile;
	}

	public static PostPostTile getWaystoneRootTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(x, y, z);
		if (ret instanceof PostPostTile) {
			return (PostPostTile) ret;
		} else {
			return null;
		}
	}

	@Override
	public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
		if (!world.isRemote) {
			return;
		}
		PostPostTile tile = getTile(world, x, y, z);
		Vec3 lookVec = player.getLookVec();
		Vec3 pos = player.getPosition(1);
		pos.xCoord = x+0.5 - pos.xCoord;
		pos.yCoord = y+0.5 - pos.yCoord - player.eyeHeight;
		pos.zCoord = z+0.5 - pos.zCoord;
		if (player.getHeldItem() != null && player.getHeldItem().getItem() instanceof PostWrench) {
			if (player.isSneaking()) {
				if (pos.yCoord / pos.lengthVector() < lookVec.yCoord) {
					tile.bases.flip1 = !tile.bases.flip1;
				} else {
					tile.bases.flip2 = !tile.bases.flip2;
				}
			} else {
				if (pos.yCoord / pos.lengthVector() < lookVec.yCoord) {
					tile.bases.rotation1 = (tile.bases.rotation1 - 15) % 360;
				} else {
					tile.bases.rotation2 = (tile.bases.rotation2 - 15) % 360;
				}
			}
			NetworkHandler.netWrap.sendToServer(new SendPostBasesMessage(tile.toPos(), tile.bases));
		}
	}

	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(!world.isRemote){
			return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
		}
		if(player.getHeldItem()!=null&&player.getHeldItem().getItem() instanceof PostWrench){
			PostPostTile tile = getTile(world, x, y, z);
			if(player.isSneaking()){
				if(hitY > 0.5){
					tile.bases.flip1 = !tile.bases.flip1;
				}else{
					tile.bases.flip2 = !tile.bases.flip2;
				}
			}else{
				if(hitY > 0.5){
					tile.bases.rotation1 = (tile.bases.rotation1+15)%360;
				}else{
					tile.bases.rotation2 = (tile.bases.rotation2+15)%360;
				}
			}
			NetworkHandler.netWrap.sendToServer(new SendPostBasesMessage(tile.toPos(), tile.bases));
		} else {
			PostPostTile tile = getTile(world, x, y, z);
			if (!player.isSneaking()) {
				BaseInfo destination = hitY > 0.5 ? tile.bases.base1 : tile.bases.base2;
				if (!(destination == null || Signpost.serverSide)) {
					NetworkHandler.netWrap.sendToServer(new TeleportMeMessage(destination));
				}
			}else{
				player.openGui(Signpost.instance, Signpost.GuiPostID, world, x, y, z);
			}
		}
		return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
	}

	@Override
	public void onBlockPreDestroy(World world, int x, int y, int z, int p_149725_5_) {
		getTile(world, x, y, z).onBlockDestroy();
		super.onBlockPreDestroy(world, x, y, z, p_149725_5_);
	}

	public int getRenderType() {
		return -1;
	}

	public boolean renderAsNormalBlock() {
		return false;
	}

	public boolean isOpaqueCube() {
		return false;
	}

	public static PostPostTile getTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(x, y, z);
		if (ret instanceof PostPostTile) {
			return (PostPostTile) ret;
		} else {
			return null;
		}
	}
}
