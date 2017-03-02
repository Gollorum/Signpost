package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.network.messages.TeleportMeMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class PostPost extends GolloBlock {
	
	public PostPost() {
		super(Material.WOOD, "post");
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		this.setHarvestLevel("axe", 0);
		this.setHardness(2);
		this.setResistance(100000);
		this.setLightOpacity(0);
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		PostPostTile tile = new PostPostTile();
		tile.isItem = false;
		return tile;
	}

	public static PostPostTile getWaystonePostTile(World world, BlockPos pos) {
		TileEntity ret = world.getTileEntity(pos);
		if (ret instanceof PostPostTile) {
			return (PostPostTile) ret;
		} else {
			return null;
		}
	}

	@Override
	public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
		if (!world.isRemote) {
			return;
		}
		PostPostTile tile = getTile(world, pos);
		Vec3d lookVec = player.getLookVec();
		BlockPos pPos = player.getPosition(); 
		pPos = pPos.subtract(new Vec3i(pos.getX()+0.5, pos.getY()+0.5 - player.eyeHeight, pos.getZ()+0.5));
		if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof PostWrench) {
			if (player.isSneaking()) {
				if (-pPos.getY() / MyBlockPos.toLength(pPos) < lookVec.yCoord) {
					tile.bases.flip1 = !tile.bases.flip1;
				} else {
					tile.bases.flip2 = !tile.bases.flip2;
				}
			} else {
				if (-pPos.getY() / MyBlockPos.toLength(pPos) < lookVec.yCoord) {
					tile.bases.rotation1 = (tile.bases.rotation1 - 15) % 360;
				} else {
					tile.bases.rotation2 = (tile.bases.rotation2 - 15) % 360;
				}
			}
			NetworkHandler.netWrap.sendToServer(new SendPostBasesMessage(tile.toPos(), tile.bases));
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(!world.isRemote){
			return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
		}
		if(player.getHeldItemMainhand()!=null&&player.getHeldItemMainhand().getItem() instanceof PostWrench){
			PostPostTile tile = getTile(world, pos);
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
			PostPostTile tile = getTile(world, pos);
			if (!player.isSneaking()) {
				BaseInfo destination = hitY > 0.5 ? tile.bases.base1 : tile.bases.base2;
				if (!(destination == null || Signpost.serverSide)) {
					NetworkHandler.netWrap.sendToServer(new TeleportMeMessage(destination));
				}
			}else{
				player.openGui(Signpost.instance, Signpost.GuiPostID, world, pos.getX(), pos.getY(), pos.getZ());
			}
		}
		return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
	}

	@Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity){
		if(!(entity instanceof EntityPlayer)){
			return false;
		}
		EntityPlayer player = ((EntityPlayer)entity);
		return !(player.getHeldItemMainhand()!=null&&player.getHeldItemMainhand().getItem() instanceof PostWrench);
	}
	
	@Override
	public void onBlockDestroyedByPlayer(World world, BlockPos pos, IBlockState state){
		getTile(world, pos).onBlockDestroy();
		super.onBlockDestroyedByPlayer(world, pos, state);
	}
	
	@Override
	public void onBlockDestroyedByExplosion(World world, BlockPos pos, Explosion explosionIn){
		getTile(world, pos).onBlockDestroy();
		super.onBlockDestroyedByExplosion(world, pos, explosionIn);
	}

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
	public BlockRenderLayer getBlockLayer(){
		return BlockRenderLayer.TRANSLUCENT;
	}

	public static PostPostTile getTile(World world, BlockPos pos) {
		return getWaystonePostTile(world, pos);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		PostPostTile tile = new PostPostTile();
		tile.isItem = false;
		return tile;
	}
}
