package gollorum.signpost.blocks;

import java.util.UUID;

import gollorum.signpost.Signpost;
import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class BasePost extends BlockContainer {

	public BasePost() {
		super(Material.ROCK);
		this.setHarvestLevel("pickaxe", 1);
		this.setHardness(2);
		this.setResistance(100000);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		this.setUnlocalizedName("SignpostBase");
		this.setRegistryName(Signpost.MODID+":blockbase");
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack heldItem, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (ConfigHandler.deactivateTeleportation) {
			return false;
		}
		if (!world.isRemote) {
			BaseInfo ws = getWaystoneRootTile(world, pos).getBaseInfo();
			if (!player.isSneaking()) {
				if (!ConfigHandler.deactivateTeleportation) {
					NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.discovered", "<Waystone>", ws.name), (EntityPlayerMP) player);
				}
				PostHandler.addDiscovered(player.getUniqueID(), ws);
			} else {
				if (!ConfigHandler.deactivateTeleportation
						&& ConfigHandler.securityLevelWaystone.canUse((EntityPlayerMP) player, ""+ws.owner)) {
					NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, pos.getX(), pos.getY(), pos.getZ()), (EntityPlayerMP) player);
				}
			}
		}
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new BasePostTile().setup();
	}

	public static BasePostTile getWaystoneRootTile(World world, BlockPos pos) {
		TileEntity ret = world.getTileEntity(pos);
		if (ret instanceof BasePostTile) {
			return (BasePostTile) ret;
		} else {
			return null;
		}
	}

	public static String generateName() {
		int i = 1;
		String ret;
		do {
			ret = "Waystone " + (i++);
		} while (PostHandler.allWaystones.nameTaken(ret));
		return ret;
	}

	public static void placeServer(World world, MyBlockPos pos, EntityPlayerMP player) {
		BasePostTile tile = getWaystoneRootTile(world, pos.toBlockPos());
		String name = generateName();
		UUID owner = player.getUniqueID();
		BaseInfo ws;
		if((ws = tile.getBaseInfo())==null){
			ws = new BaseInfo(name, pos, owner);
			PostHandler.allWaystones.add(ws);
		}else{
			ws.setAll(new BaseInfo(name, pos, owner));
		}
		PostHandler.addDiscovered(player.getUniqueID(), ws);
		NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.PLACED, world, pos.x, pos.y, pos.z, name));
	}

	public static void placeClient(final World world, final MyBlockPos pos, final EntityPlayer player) {
		BasePostTile tile = getWaystoneRootTile(world, pos.toBlockPos());
		if (tile != null && tile.getBaseInfo() == null) {
			PostHandler.allWaystones.add(new BaseInfo("", pos, player.getUniqueID()));
		}
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new BasePostTile();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return true;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return true;
	}

	@Override
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.SOLID;
	}

}
