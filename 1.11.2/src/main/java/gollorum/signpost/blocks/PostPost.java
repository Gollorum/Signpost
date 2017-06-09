package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.Sign.OverlayType;
import gollorum.signpost.util.math.tracking.Cuboid;
import gollorum.signpost.util.math.tracking.DDDVector;
import gollorum.signpost.util.math.tracking.Intersect;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PostPost extends SuperPostPost {

	public PostType type;

	public static enum PostType{
						OAK(	Material.WOOD, 	"sign_oak", 	"planks_oak",		Item.getItemFromBlock(Blocks.PLANKS),		0),
						SPRUCE(	Material.WOOD, 	"sign_spruce", 	"planks_spruce",	Item.getItemFromBlock(Blocks.PLANKS),		1),
						BIRCH(	Material.WOOD, 	"sign_birch", 	"planks_birch",		Item.getItemFromBlock(Blocks.PLANKS),		2),
						JUNGLE(	Material.WOOD,	"sign_jungle", 	"planks_jungle",	Item.getItemFromBlock(Blocks.PLANKS),		3),
						ACACIA(	Material.WOOD, 	"sign_acacia", 	"planks_acacia",	Item.getItemFromBlock(Blocks.PLANKS),		4),
						BIGOAK(	Material.WOOD, 	"sign_big_oak", "planks_big_oak",	Item.getItemFromBlock(Blocks.PLANKS),		5),
						IRON(	Material.IRON, 	"sign_iron", 	"iron_block",		Items.IRON_INGOT,							0),
						STONE(	Material.ROCK, 	"sign_stone", 	"stone",			Item.getItemFromBlock(Blocks.STONE),		0);
		public Material material;
		public ResourceLocation texture;
		public String textureMain;
		public ResourceLocation resLocMain;
		public Item baseItem;
		public int metadata;

		private PostType(Material material, String texture, String textureMain, Item baseItem, int metadata) {
			this.material = material;
			this.texture = new ResourceLocation(Signpost.MODID + ":textures/blocks/"+texture+".png");
			this.textureMain = textureMain;
			this.resLocMain = new ResourceLocation("minecraft:textures/blocks/"+textureMain+".png");
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
		super(Material.WOOD);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		this.setHarvestLevel("axe", 0);
		this.setHardness(2);
		this.setResistance(100000);
		this.setLightOpacity(0);
		this.setUnlocalizedName("SignpostPostOAK");
		this.setRegistryName(Signpost.MODID+":blockpostoak");
	}

	public PostPost(PostType type){
		super(type.material);
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
		this.setHardness(2);
		this.setResistance(100000);
		this.setLightOpacity(0);
		this.setUnlocalizedName("SignpostPost"+type.name());
		this.setRegistryName(Signpost.MODID+":blockpost"+type.name().toLowerCase());
	}
	
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		PostPostTile tile = new PostPostTile(type);
		return tile;
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		PostPostTile tile = new PostPostTile(type);
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
	public void clickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		Hit hit = (Hit)hitObj;
		DoubleBaseInfo tilebases = ((PostPostTile)superTile).getBases();
		if (hit.target == HitTarget.BASE1) {
			tilebases.sign1.rotation = (tilebases.sign1.rotation - 15) % 360;
		} else if(hit.target == HitTarget.BASE2) {
			tilebases.sign2.rotation = (tilebases.sign2.rotation - 15) % 360;
		}
	}

	@Override
	public void rightClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		Hit hit = (Hit)hitObj;
		DoubleBaseInfo tilebases = ((PostPostTile)superTile).getBases();
		if (hit.target == HitTarget.BASE1) {
			tilebases.sign1.rotation = (tilebases.sign1.rotation + 15) % 360;
		} else if (hit.target == HitTarget.BASE2) {
			tilebases.sign2.rotation = (tilebases.sign2.rotation + 15) % 360;
		}
	}

	@Override
	public void shiftClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		Hit hit = (Hit)hitObj;
		DoubleBaseInfo tilebases = ((PostPostTile)superTile).getBases();
		if (hit.target == HitTarget.BASE1) {
			tilebases.sign1.flip = !tilebases.sign1.flip;
		} else if(hit.target == HitTarget.BASE2) {
			tilebases.sign2.flip = !tilebases.sign2.flip;
		}
	}
	
	@Override
	public void rightClickBrush(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z){
		NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostBrushID, x, y, z), (EntityPlayerMP) player);
	}

	public void clickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z){
		Sign sign = getSignByHit((Hit)hitObj, (PostPostTile) superTile);
		if(sign != null){
			sign.rotation = (sign.flip?90:270) - (int) (player.rotationYawHead);
		}
	}
	
	public void rightClickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z){
		Hit hit = (Hit)hitObj;
		if(hit.target.equals(HitTarget.BASE1)||hit.target.equals(HitTarget.BASE2)){
			NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostRotationID, x, y, z), (EntityPlayerMP) player);
		}
	}
	
	public void shiftClickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z){
		Sign sign = getSignByHit((Hit)hitObj, (PostPostTile) superTile);
		if(sign != null){
			sign.rotation = (sign.flip?270:90) - (int) (player.rotationYawHead);
		}
	}
	
	@Override
	public void click(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		Hit hit = (Hit)hitObj;
		PostPostTile tile = (PostPostTile)superTile;
		DoubleBaseInfo tilebases = tile.getBases();
		if (hit.target == HitTarget.BASE1) {
			if(tilebases.sign1.overlay != null){
				player.inventory.addItemStackToInventory(new ItemStack(tilebases.sign1.overlay.item, 1));
			}
		} else if(hit.target == HitTarget.BASE2) {
			if(tilebases.sign2.overlay != null){
				player.inventory.addItemStackToInventory(new ItemStack(tilebases.sign2.overlay.item, 1));
			}
		}
		for(OverlayType now: OverlayType.values()){
			if(player.getHeldItemMainhand().getItem().getClass() == now.item.getClass()){
				if (hit.target == HitTarget.BASE1) {
					tilebases.sign1.overlay = now;
				} else if(hit.target == HitTarget.BASE2) {
					tilebases.sign2.overlay = now;
				}
				player.inventory.clearMatchingItems(now.item, 0, 1, null);
				NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
				return;
			}
		}
		if (hit.target == HitTarget.BASE1) {
			tilebases.sign1.overlay = null;
		} else if(hit.target == HitTarget.BASE2) {
			tilebases.sign2.overlay = null;
		}
		NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
	}

	@Override
	public void rightClick(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		Hit hit = (Hit)hitObj;
		PostPostTile tile = (PostPostTile)superTile;
		if (hit.target != HitTarget.POST) {
			if (ConfigHandler.deactivateTeleportation) {
				return;
			}
			BaseInfo destination = hit.target == HitTarget.BASE1 ? tile.getBases().sign1.base : tile.getBases().sign2.base;
			if (destination != null) {
				if(destination.pos==null){
					NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.noTeleport"), (EntityPlayerMP) player);
				}else{
					int stackSize = PostHandler.getStackSize(destination.pos, tile.toPos());
					if(PostHandler.canPay(player, destination.pos.x, destination.pos.y, destination.pos.z, x, y, z)){
						PostHandler.teleportMe(destination, (EntityPlayerMP) player, stackSize);
					}else{
						String[] keyword = { "<itemName>", "<amount>" };
						String[] replacement = { ConfigHandler.cost.getUnlocalizedName() + ".name",	"" + stackSize };
						NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.payment", keyword, replacement), (EntityPlayerMP) player);
					}
				}
			}
		} else {
			NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostID, x, y, z), (EntityPlayerMP) player);
		}
	}

	@Override
	public void shiftClick(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		Hit hit = (Hit)hitObj;
		DoubleBaseInfo tilebases = ((PostPostTile)superTile).getBases();
		if (hit.target == HitTarget.BASE1) {
			tilebases.sign1.point = !tilebases.sign1.point;
		} else if(hit.target == HitTarget.BASE2) {
			tilebases.sign2.point = !tilebases.sign2.point;
		}
	}

	@Override
	public void clickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		Hit hit = (Hit)hitObj;
		PostPostTile tile = (PostPostTile)superTile;
		DoubleBaseInfo tilebases = tile.getBases();
		if (hit.target == HitTarget.BASE1) {
			tilebases.sign1.point = !tilebases.sign1.point;
			NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
		} else if(hit.target == HitTarget.BASE2) {
			tilebases.sign2.point = !tilebases.sign2.point;
			NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
		}
	}

	@Override
	public void shiftClickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		shiftClick(hitObj, superTile, player, x, y, z);
	}

	@Override
	public void sendPostBasesToAll(SuperPostPostTile superTile) {
		PostPostTile tile = (PostPostTile)superTile;
		DoubleBaseInfo tilebases = tile.getBases();
		NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
	}

	@Override
	public void sendPostBasesToServer(SuperPostPostTile superTile) {
		PostPostTile tile = (PostPostTile)superTile;
		DoubleBaseInfo tilebases = tile.getBases();
		NetworkHandler.netWrap.sendToServer(new SendPostBasesMessage(tile, tilebases));
	}
	
	@Override
	public Object getHitTarget(World world, int x, int y, int z, EntityPlayer player){
		DDDVector head = new DDDVector(player.posX, player.posY, player.posZ);
		head.y+=player.getEyeHeight();
		if(player.isSneaking())
			head.y-=0.08;
		Vec3d look = player.getLookVec();
		DoubleBaseInfo bases = getWaystonePostTile(world, new BlockPos(x, y, z)).getBases();
		DDDVector rotPos = new DDDVector(x+0.5,y+0.5,z+0.5);
		DDDVector signPos;
		DDDVector edges = new DDDVector(1.4375, 0.375, 0.0625);
		
		if(bases.sign1.flip){
			signPos = new DDDVector(x-0.375, y+0.5625, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.5625, z+0.625);
		}
		Cuboid sign1 = new Cuboid(signPos, edges, bases.sign1.calcRot(x, z), rotPos);
		
		if(bases.sign2.flip){
			signPos = new DDDVector(x-0.375, y+0.0625, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.0625, z+0.625);
		}
		Cuboid sign2 = new Cuboid(signPos, edges, bases.sign2.calcRot(x, z), rotPos);
		Cuboid post = new Cuboid(new DDDVector(x+0.375, y, z+0.375), new DDDVector(0.25, 1, 0.25), 0);

		DDDVector start = new DDDVector(head);
		DDDVector end = start.add(new DDDVector(look.xCoord, look.yCoord, look.zCoord));
		Intersect sign1Hit = sign1.traceLine(start, end, true);
		Intersect sign2Hit = sign2.traceLine(start, end, true);
		Intersect postHit = post.traceLine(start, end, true);
		double sign1Dist = sign1Hit.exists&&bases.sign1.base!=null&&bases.sign1.base.hasName()?sign1Hit.pos.distance(start):Double.MAX_VALUE;
		double sign2Dist = sign2Hit.exists&&bases.sign2.base!=null&&bases.sign2.base.hasName()?sign2Hit.pos.distance(start):Double.MAX_VALUE;
		double postDist = postHit.exists?postHit.pos.distance(start):Double.MAX_VALUE;
		double dist;
		HitTarget target;
		DDDVector pos;
		if(sign1Dist<sign2Dist){
			dist = sign1Dist;
			pos = sign1Hit.pos;
			target = HitTarget.BASE1;
		}else{
			dist = sign2Dist;
			pos = sign2Hit.pos;
			target = HitTarget.BASE2;
		}
		if(postDist<=dist){
			dist = postDist;
			pos = postHit.pos;
			target = HitTarget.POST;
		}
		return new Hit(target, pos);
	}

	public static PostPostTile getTile(World world, BlockPos pos) {
		return getWaystonePostTile(world, pos);
	}

	public Sign getSignByHit(Hit hit, PostPostTile tile){
		if(hit.target.equals(HitTarget.BASE1)){
			return tile.getBases().sign1;
		}else if(hit.target.equals(HitTarget.BASE2)){
			return tile.getBases().sign2;
		}else{
			return null;
		}
	}
}
