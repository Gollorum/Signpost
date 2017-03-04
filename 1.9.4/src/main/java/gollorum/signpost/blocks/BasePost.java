package gollorum.signpost.blocks;

import java.util.UUID;

import javax.annotation.Nullable;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.SendDiscoveredToServerMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.resources.data.LanguageMetadataSection;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.util.text.translation.LanguageMap;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class BasePost extends GolloBlock {

	public BasePost() {
		super(Material.ROCK, "base");
		this.setHarvestLevel("pickaxe", 1);
		this.setHardness(2);
		this.setResistance(100000);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!world.isRemote) {
			return false;
		}
		BaseInfo ws = getWaystoneRootTile(world, pos).ws;
		if (!player.isSneaking()) {
			String out = I18n.translateToLocal("signpost.discovered");
			out = out.replaceAll("<Waystone>", ws.name);
			Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentString(out));
			NetworkHandler.netWrap.sendToServer(new SendDiscoveredToServerMessage(ws.name));
		} else {
			player.openGui(Signpost.instance, Signpost.GuiBaseID, world, pos.getX(), pos.getY(), pos.getZ());
		}

		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new BasePostTile();
	}

	public static BasePostTile getWaystoneRootTile(World world, BlockPos pos) {
		TileEntity ret = world.getTileEntity(pos);
		if (ret instanceof BasePostTile) {
			return (BasePostTile) ret;
		} else {
			return null;
		}
	}

	@Override
	public void onBlockPlacedBy(final World world, final BlockPos inPos, IBlockState state,
			final EntityLivingBase entity, ItemStack stack) {
		if (entity instanceof EntityPlayerMP) {
			BasePostTile tile = getWaystoneRootTile(world, inPos);
			String name = "Waystone" + inPos.getX() + inPos.getY() + inPos.getZ();
			MyBlockPos pos = new MyBlockPos(world, inPos, entity.dimension);
			UUID owner = entity.getUniqueID();
			tile.ws = new BaseInfo(name, pos, owner);
			if (PostHandler.updateWS(tile.ws, false)) {
				PostHandler.addDiscovered(entity.getUniqueID(), tile.ws);
				NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage().init());
			} else {
				System.out.println(
						"Dies ist ein Fehler und wird deshalb niemals auftreten. Ich bin also nur Einbildung :D");
			}
		} else {
			SPEventHandler.scheduleTask(new Runnable() {
				@Override
				public void run() {
					BasePostTile tile = getWaystoneRootTile(world, inPos);
					if (tile != null && tile.ws == null) {
						String name = "Waystone " + inPos.getX() + inPos.getY() + inPos.getZ();
						MyBlockPos pos = new MyBlockPos("", inPos, entity.dimension);
						UUID owner = entity.getUniqueID();
						for (BaseInfo now : PostHandler.allWaystones) {
							if (now.pos.equals(pos)) {
								tile.ws = now;
								return;
							}
						}
						tile.ws = new BaseInfo(name, pos, owner);
						PostHandler.updateWS(tile.ws, false);
					}
				}
			}, 20);
		}
	}

	public static void onBlockDestroy(MyBlockPos pos) {
		System.out.println("DESASTRÖS!");
		if (PostHandler.allWaystones.removeBaseInfo(pos)) {
			System.out.println("is weg");
			NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage().init());
		}else{
			System.out.println("is nich weg");
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
