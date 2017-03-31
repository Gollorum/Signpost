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
import gollorum.signpost.util.DoubleBaseInfo.OverlayType;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.math.tracking.Cuboid;
import gollorum.signpost.util.math.tracking.DDDVector;
import gollorum.signpost.util.math.tracking.Intersect;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
						BIGOAK(	Material.WOOD, 	"post_bigoak",	"planks_big_oak",	Item.getItemFromBlock(Blocks.PLANKS),		5),
						IRON(	Material.IRON, 	"post_iron", 	"iron_block",		Items.IRON_INGOT,							0),
						STONE(	Material.ROCK, 	"post_stone", 	"stone",			Item.getItemFromBlock(Blocks.STONE),		0);
		public Material material;
		public ResourceLocation texture;
		public String textureMain;
		public Item baseItem;
		public int metadata;

		private PostType(Material material, String texture, String textureMain, Item baseItem, int metadata) {
			this.material = material;
			this.texture = new ResourceLocation(Signpost.MODID + ":textures/blocks/"+texture+".png");
			this.textureMain = textureMain;
			this.baseItem = baseItem;
			this.metadata = metadata;
		}
	}

	public static enum HitTarget{BASE1, BASE2, POST;}
	
	public static class Hit{
		public HitTarget target;
		public DDDVector pos;
		public Hit(HitTarget target, DDDVector pos){
			this.target = target; this.pos = pos;
		}
	}
	
	@Deprecated
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
		if (world.isRemote||!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)) {
			return;
		}
		Hit hit = getHitTarget(world, pos, player);	
		if(hit.target == HitTarget.POST){
			return;
		}
		PostPostTile tile = getTile(world, pos);
		if (!player.getHeldItemMainhand().getItem().equals(Items.AIR)){
			Item item = player.getHeldItemMainhand().getItem();
			if(item instanceof ItemBlock && doThingsWithItem(item, hit, tile)){
				NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tile.getBases()));
				return;
			}
			if(item instanceof PostWrench) {
				DoubleBaseInfo tilebases = tile.getBases();
				if (player.isSneaking()) {
					if (hit.target == HitTarget.BASE1) {
						tilebases.flip1 = !tilebases.flip1;
					} else {
						tilebases.flip2 = !tilebases.flip2;
					}
				} else {
					if (hit.target == HitTarget.BASE1) {
						tilebases.rotation1 = (tilebases.rotation1 - 15) % 360;
					} else {
						tilebases.rotation2 = (tilebases.rotation2 - 15) % 360;
					}
				}
				if(ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)) {
					NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
				}
			}else{
				if (player.isSneaking()) {
					DoubleBaseInfo tilebases = tile.getBases();
					if (hit.target == HitTarget.BASE1) {
						tilebases.point1 = !tilebases.point1;
					} else {
						tilebases.point2 = !tilebases.point2;
					}
				}else{
					DoubleBaseInfo tilebases = tile.getBases();
					if (hit.target == HitTarget.BASE1) {
						if(tilebases.overlay1 != null){
							player.inventory.addItemStackToInventory(new ItemStack(tilebases.overlay1.item, 1));
						}
					}else{
						if(tilebases.overlay2 != null){
							player.inventory.addItemStackToInventory(new ItemStack(tilebases.overlay2.item, 1));
						}
					}
					for(OverlayType now: OverlayType.values()){
						if(item.getClass() == now.item.getClass()){
							if (hit.target == HitTarget.BASE1) {
								tilebases.overlay1 = now;
							}else{
								tilebases.overlay2 = now;
							}
							player.inventory.clearMatchingItems(now.item, 0, 1, null);
							NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
							return;
						}
					}
					if (hit.target == HitTarget.BASE1) {
						tilebases.overlay1 = null;
					}else{
						tilebases.overlay2 = null;
					}
					NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
				}
			}
		}else{
			DoubleBaseInfo tilebases = tile.getBases();
			if (hit.target == HitTarget.BASE1) {
				tilebases.point1 = !tilebases.point1;
				NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
			}else{
				tilebases.point2 = !tilebases.point2;
				NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
			}
		}
	}

	protected boolean doThingsWithItem(Item item, Hit hit, PostPostTile tile) {
		return false;
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if (world.isRemote) {
			return true;
		}
		if (player.getHeldItemMainhand().getItem() instanceof PostWrench) {
			if(!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)){
				return true;
			}
			PostPostTile tile = getTile(world, pos);
			DoubleBaseInfo tilebases = tile.getBases();
			Hit hit = getHitTarget(world, pos, player);
			if (hit.target == HitTarget.BASE1) {
				tilebases.rotation1 = (tilebases.rotation1 + 15) % 360;
			} else if (hit.target == HitTarget.BASE2) {
				tilebases.rotation2 = (tilebases.rotation2 + 15) % 360;
			} else if (hit.target == HitTarget.POST){
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostID, pos.getX(), pos.getY(), pos.getZ()), (EntityPlayerMP) player);
				return true;
			}
			NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
		} else {
			PostPostTile tile = getTile(world, pos);
			Hit hit = getHitTarget(world, pos, player);
			if (hit.target != HitTarget.POST) {
				if (ConfigHandler.deactivateTeleportation) {
					return true;
				}
				BaseInfo destination = hit.target == HitTarget.BASE1 ? tile.getBases().base1 : tile.getBases().base2;
				if (destination != null) {
					if (ConfigHandler.cost == null) {
						PostHandler.teleportMe(destination, (EntityPlayerMP) player, 0);
					} else {
						int stackSize = (int) destination.pos.distance(tile.toPos()) / ConfigHandler.costMult + 1;
						if (player.getHeldItemMainhand().getItem().getClass() == ConfigHandler.cost.getClass()
						 && player.getHeldItemMainhand().getCount() >= stackSize) {
							PostHandler.teleportMe(destination, (EntityPlayerMP) player, stackSize);
						} else {
							String[] keyword = { "<itemName>", "<amount>" };
							String[] replacement = { ConfigHandler.cost.getUnlocalizedName() + ".name",	"" + stackSize };
							NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.payment", keyword, replacement), (EntityPlayerMP) player);
						}
					}
				}
			} else {
				if(!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)){
					return true;
				}
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostID, pos.getX(), pos.getY(), pos.getZ()), (EntityPlayerMP) player);
			}
		}
		return true;
	}

	public Hit getHitTarget(World world, BlockPos pos, EntityPlayer/*MP*/ player){
		DDDVector head = new DDDVector(player.posX, player.posY, player.posZ);
		head.y+=player.getEyeHeight();
		if(player.isSneaking())
			head.y-=0.08;
		Vec3d look = player.getLookVec();
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		DoubleBaseInfo bases = getWaystonePostTile(world, pos).getBases();
		DDDVector rotPos = new DDDVector(x+0.5,y+0.5,z+0.5);
		DDDVector signPos;
		DDDVector edges = new DDDVector(1.4375, 0.375, 0.0625);
		
		if(bases.flip1){
			signPos = new DDDVector(x-0.375, y+0.5625, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.5625, z+0.625);
		}
		Cuboid sign1 = new Cuboid(signPos, edges, PostPostTile.calcRot1(bases, x, z), rotPos);
		
		if(bases.flip2){
			signPos = new DDDVector(x-0.375, y+0.0625, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.0625, z+0.625);
		}
		Cuboid sign2 = new Cuboid(signPos, edges, PostPostTile.calcRot2(bases, x, z), rotPos);
		Cuboid post = new Cuboid(new DDDVector(x+0.375, y, z+0.375), new DDDVector(0.25, 1, 0.25), 0);

		DDDVector start = new DDDVector(head);
		DDDVector end = start.add(new DDDVector(look.xCoord, look.yCoord, look.zCoord));
		Intersect sign1Hit = sign1.traceLine(start, end, true);
		Intersect sign2Hit = sign2.traceLine(start, end, true);
		Intersect postHit = post.traceLine(start, end, true);
		double sign1Dist = sign1Hit.exists&&bases.base1!=null?sign1Hit.pos.distance(start):Double.MAX_VALUE;
		double sign2Dist = sign2Hit.exists&&bases.base2!=null?sign2Hit.pos.distance(start):Double.MAX_VALUE;
		double postDist = postHit.exists?postHit.pos.distance(start):Double.MAX_VALUE/2;
		double dist;
		HitTarget target;
		DDDVector pos2;
		if(sign1Dist<sign2Dist){
			dist = sign1Dist;
			pos2 = sign1Hit.pos;
			target = HitTarget.BASE1;
		}else{
			dist = sign2Dist;
			pos2 = sign2Hit.pos;
			target = HitTarget.BASE2;
		}
		if(postDist<dist){
			dist = postDist;
			pos2 = postHit.pos;
			target = HitTarget.POST;
		}
		return new Hit(target, pos2);
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
