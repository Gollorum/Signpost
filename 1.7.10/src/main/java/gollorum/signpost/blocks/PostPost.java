package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class PostPost extends BlockContainer {
	
	public PostType type;

	public static enum PostType{
						OAK(	Material.wood, 	"sign", 		"planks_oak",		Item.getItemFromBlock(Blocks.planks),		0),
						SPRUCE(	Material.wood, 	"sign_spruce", 	"planks_spruce",	Item.getItemFromBlock(Blocks.planks),		1),
						BIRCH(	Material.wood, 	"sign_birch", 	"planks_birch",		Item.getItemFromBlock(Blocks.planks),		2),
						JUNGLE(	Material.wood,	"sign_jungle", 	"planks_jungle",	Item.getItemFromBlock(Blocks.planks),		3),
						ACACIA(	Material.wood, 	"sign_acacia", 	"planks_acacia",	Item.getItemFromBlock(Blocks.planks),		4),
						BIGOAK(	Material.wood, 	"sign_big_oak", "planks_big_oak",	Item.getItemFromBlock(Blocks.planks),		5),
						IRON(	Material.iron, 	"sign_iron", 	"iron_block",		Items.iron_ingot,							0),
						STONE(	Material.rock, 	"sign_stone", 	"stone",			Item.getItemFromBlock(Blocks.stone),		0);
		public Material material;
		public String texture;
		public String textureMain;
		public Item baseItem;
		public int metadata;

		private PostType(Material material, String texture, String textureMain, Item baseItem, int metadata) {
			this.material = material;
			this.texture = texture;
			this.textureMain = textureMain;
			this.baseItem = baseItem;
			this.metadata = metadata;
		}
	}

	@Deprecated
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
	
	public PostPost(PostType type){
		super(type.material);
		this.type = type;
		setBlockName("SignpostPost"+type.toString());
		setCreativeTab(CreativeTabs.tabTransport);
		setBlockTextureName("Minecraft:"+type.textureMain);
		this.setHardness(2);
		this.setResistance(100000);
		float f = 15F / 32;
		this.setBlockBounds(0.5f - f, 0.0F, 0.5F - f, 0.5F + f, 1, 0.5F + f);
	}
	
	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
		PostPostTile tile = new PostPostTile(type);
		return tile;
	}

	public static PostPostTile getWaystonePostTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(x, y, z);
		if (ret instanceof PostPostTile) {
			return (PostPostTile) ret;
		} else {
			return null;
		}
	}

	@Override
	public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
		if (world.isRemote||!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)) {
			return;
		}
		PostPostTile tile = getTile(world, x, y, z);
		Vec3 lookVec = player.getLookVec();
		Vec3 pos = player.getPosition(1);
		pos.xCoord = x + 0.5 - pos.xCoord;
		pos.yCoord = y + 0.5 - pos.yCoord - player.eyeHeight;
		pos.zCoord = z + 0.5 - pos.zCoord;
		if (player.getHeldItem() != null && player.getHeldItem().getItem() instanceof PostWrench) {
			DoubleBaseInfo tilebases = tile.getBases();
			if (player.isSneaking()) {
				if (pos.yCoord / pos.lengthVector() < lookVec.yCoord) {
					tilebases.flip1 = !tilebases.flip1;
				} else {
					tilebases.flip2 = !tilebases.flip2;
				}
			} else {
				if (pos.yCoord / pos.lengthVector() < lookVec.yCoord) {
					tilebases.rotation1 = (tilebases.rotation1 - 15) % 360;
				} else {
					tilebases.rotation2 = (tilebases.rotation2 - 15) % 360;
				}
			}
			NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile.toPos(), tilebases));
		}
	}

	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		}
		if (player.getHeldItem() != null && player.getHeldItem().getItem() instanceof PostWrench) {
			if(!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)){
				return true;
			}
			PostPostTile tile = getTile(world, x, y, z);
			DoubleBaseInfo tilebases = tile.getBases();
			if (player.isSneaking()) {
				if (hitY > 0.5) {
					tilebases.flip1 = !tilebases.flip1;
				} else {
					tilebases.flip2 = !tilebases.flip2;
				}
			} else {
				if (hitY > 0.5) {
					tilebases.rotation1 = (tilebases.rotation1 + 15) % 360;
				} else {
					tilebases.rotation2 = (tilebases.rotation2 + 15) % 360;
				}
			}
			NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile.toPos(), tilebases));
		} else {
			PostPostTile tile = getTile(world, x, y, z);
			if (!player.isSneaking()) {
				if (ConfigHandler.deactivateTeleportation) {
					return true;
				}
				BaseInfo destination = hitY > 0.5 ? tile.getBases().base1 : tile.getBases().base2;
				if (destination != null) {
					if (ConfigHandler.cost == null) {
						PostHandler.teleportMe(destination, (EntityPlayerMP) player, 0);
					} else {
						int stackSize = (int) destination.pos.distance(tile.toPos()) / ConfigHandler.costMult + 1;
						if (player.getHeldItem() != null
								&& player.getHeldItem().getItem().getClass() == ConfigHandler.cost.getClass()
								&& player.getHeldItem().stackSize >= stackSize) {
							PostHandler.teleportMe(destination, (EntityPlayerMP) player, stackSize);
						} else {
							String[] keyword = { "<itemName>", "<amount>" };
							String[] replacement = { ConfigHandler.cost.getUnlocalizedName() + ".name",
									"" + stackSize };
							NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.payment", keyword, replacement),
									(EntityPlayerMP) player);
						}
					}
				}
			} else {
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostID, x, y, z), (EntityPlayerMP) player);
			}
		}
		return true;
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

	public static void placeClient(World world, BlockPos blockPos, EntityPlayer player) {
		// TODO Auto-generated method stub

	}

	public static void placeServer(World world, BlockPos blockPos, EntityPlayerMP player) {
		// TODO Auto-generated method stub

	}
}
