package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.network.messages.SendAllPostBasesMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class PostPost extends GolloBlock {

	public PostType type;

	public static enum PostType{
						OAK(	Material.WOOD, 	"post", 		"planks_oak",		Item.getItemFromBlock(Blocks.PLANKS),		0),
						SPRUCE(	Material.WOOD, 	"post_spruce", 	"planks_spruce",	Item.getItemFromBlock(Blocks.PLANKS),		1),
						BIRCH(	Material.WOOD, 	"post_birch", 	"planks_birch",		Item.getItemFromBlock(Blocks.PLANKS),		2),
						JUNGLE(	Material.WOOD,	"post_jungle", 	"planks_jungle",	Item.getItemFromBlock(Blocks.PLANKS),		3),
						ACACIA(	Material.WOOD, 	"post_acacia", 	"planks_acacia",	Item.getItemFromBlock(Blocks.PLANKS),		4),
						BIGOAK(	Material.WOOD, 	"post_big_oak", "planks_big_oak",	Item.getItemFromBlock(Blocks.PLANKS),		5),
						IRON(	Material.IRON, 	"post_iron", 	"iron_block",		Items.IRON_INGOT,							0),
						STONE(	Material.ROCK, 	"post_stone", 	"stone",			Item.getItemFromBlock(Blocks.STONE),		0);
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

	public PostPost() {
		super(Material.WOOD, "postoak");
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		this.setHarvestLevel("axe", 0);
		this.setHardness(2);
		this.setResistance(100000);
		this.setLightOpacity(0);
	}

	public PostPost(PostType type){
		super(Material.WOOD, "post"+type.name().toLowerCase());
		this.type = type;
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		switch(type){
		case STONE:
			this.setHarvestLevel("pickaxe", 0);
			break;
		case IRON:
			this.setHarvestLevel("pickaxe", 1);
			break;
		default:
			this.setHarvestLevel("axe", 0);
			break;
		}
		this.setHarvestLevel("axe", 0);
		this.setHardness(2);
		this.setResistance(100000);
		this.setLightOpacity(0);
	}
	
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		PostPostTile tile = new PostPostTile(type);
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
		if (world.isRemote||ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)) {
			return;
		}
		PostPostTile tile = getTile(world, pos);
		double lookY  = player.getLookVec().yCoord;
		double playerX = player.posX;
		double playerY = player.posY+player.eyeHeight;
		double playerZ = player.posZ;
		if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof PostWrench) {
			double deltaY = MyBlockPos.normalizedY(pos.getX()+0.5-playerX, pos.getY()+0.5-playerY, pos.getZ()+0.5-playerZ);
			DoubleBaseInfo tilebases = tile.getBases();
			if (player.isSneaking()) {
				if (deltaY < lookY) {
					tilebases.flip1 = !tilebases.flip1;
				} else {
					tilebases.flip2 = !tilebases.flip2;
				}
			} else {
				if (deltaY < lookY) {
					tilebases.rotation1 = (tilebases.rotation1 - 15) % 360;
				} else {
					tilebases.rotation2 = (tilebases.rotation2 - 15) % 360;
				}
			}
			NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile.toPos(), tilebases));
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack heldItem, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		}
		if(player.getHeldItemMainhand()!=null&&player.getHeldItemMainhand().getItem() instanceof PostWrench){
			if(!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)){
				return true;
			}
			PostPostTile tile = getTile(world, pos);
			DoubleBaseInfo tilebases = tile.getBases();
			if(player.isSneaking()){
				if(hitY > 0.5){
					tilebases.flip1 = !tilebases.flip1;
				}else{
					tilebases.flip2 = !tilebases.flip2;
				}
			}else{
				if(hitY > 0.5){
					tilebases.rotation1 = (tilebases.rotation1+15)%360;
				}else{
					tilebases.rotation2 = (tilebases.rotation2+15)%360;
				}
			}
			NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile.toPos(), tilebases));
		} else {
			PostPostTile tile = getTile(world, pos);
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
						if (player.getHeldItemMainhand() != null
								&& player.getHeldItemMainhand().getItem().getClass() == ConfigHandler.cost.getClass()
								&& player.getHeldItemMainhand().stackSize >= stackSize) {
							PostHandler.teleportMe(destination, (EntityPlayerMP) player, stackSize);
						} else {
							String[] keyword = { "<itemName>", "<amount>" };
							String[] replacement = { ConfigHandler.cost.getUnlocalizedName() + ".name", "" + stackSize };
							NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.payment", keyword, replacement), (EntityPlayerMP) player);
						}
					}
				}
			} else {
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostID, pos.getX(), pos.getY(), pos.getZ()), (EntityPlayerMP) player);
			}
		}
		return true;
	}

	@Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity){
		if(!(entity instanceof EntityPlayer)){
			return false;
		}
		EntityPlayer player = ((EntityPlayer)entity);
		return !(player.getHeldItemMainhand()!=null&&player.getHeldItemMainhand().getItem() instanceof PostWrench);
	}

	public static void onBlockDestroy(MyBlockPos pos) {
		if(PostHandler.posts.remove(pos)!=null){
			NetworkHandler.netWrap.sendToAll(new SendAllPostBasesMessage());
		}
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

	public static void placeServer(World world, MyBlockPos myBlockPos, EntityPlayerMP player) {
		// TODO Auto-generated method stub
		
	}

	public static void placeClient(World world, MyBlockPos myBlockPos, EntityPlayer player) {
		// TODO Auto-generated method stub
		
	}
}
