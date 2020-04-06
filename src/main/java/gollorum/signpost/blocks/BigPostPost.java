package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.network.messages.SendAllWaystoneNamesMessage;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.Paintable;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.Sign.OverlayType;
import gollorum.signpost.util.math.tracking.Cuboid;
import gollorum.signpost.util.math.tracking.DDDVector;
import gollorum.signpost.util.math.tracking.Intersect;
import net.minecraft.block.material.Material;
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

public class BigPostPost extends SuperPostPost {

	public BigPostType type;

	public enum BigPostType{
						OAK(	Material.WOOD, 	"bigsign_oak", 		"log_oak",		Item.getItemFromBlock(Blocks.LOG),		0),
						SPRUCE(	Material.WOOD, 	"bigsign_spruce", 	"log_spruce",	Item.getItemFromBlock(Blocks.LOG),		1),
						BIRCH(	Material.WOOD, 	"bigsign_birch", 	"log_birch",	Item.getItemFromBlock(Blocks.LOG),		2),
						JUNGLE(	Material.WOOD,	"bigsign_jungle", 	"log_jungle",	Item.getItemFromBlock(Blocks.LOG),		3),
						ACACIA(	Material.WOOD, 	"bigsign_acacia", 	"log_acacia",	Item.getItemFromBlock(Blocks.LOG2),		0),
						BIGOAK(	Material.WOOD, 	"bigsign_big_oak", 	"log_big_oak",	Item.getItemFromBlock(Blocks.LOG2),		1),
						IRON(	Material.IRON, 	"bigsign_iron", 	"iron_block",		Items.IRON_INGOT,							0),
						STONE(	Material.ROCK, 	"bigsign_stone", 	"stone",			Item.getItemFromBlock(Blocks.STONE),		0);
		public Material material;
		public ResourceLocation texture;
		public String textureMain;
		public ResourceLocation resLocMain;
		public Item baseItem;
		public int metadata;

		BigPostType(Material material, String texture, String textureMain, Item baseItem, int metadata) {
			this.material = material;
			this.texture = new ResourceLocation(Signpost.MODID + ":textures/blocks/"+texture+".png");
			this.textureMain = textureMain;
			this.resLocMain = new ResourceLocation("minecraft:textures/blocks/"+textureMain+".png");
			this.baseItem = baseItem;
			this.metadata = metadata;
		}
	}
	
	public enum BigHitTarget{BASE, POST, STONE}
	
	public static class BigHit{
		public BigHitTarget target;
		public DDDVector pos;
		public BigHit(BigHitTarget target, DDDVector pos){
			this.target = target; this.pos = pos;
		}
	}

	@Deprecated
	public BigPostPost() {
		super(Material.WOOD);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		this.setHarvestLevel("axe", 0);
		this.setHardness(2);
		this.setResistance(100000);
		this.setLightOpacity(0);
		this.setTranslationKey("SignpostBigPostOAK");
		this.setRegistryName(Signpost.MODID+":blockbigpostoak");
	}

	public BigPostPost(BigPostType type){
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
		this.setTranslationKey("SignpostBigPost"+type.name());
		this.setRegistryName(Signpost.MODID+":blockbigpost"+type.name().toLowerCase());
	}
	
	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
		BigPostPostTile tile = new BigPostPostTile(type);
		return tile;
	}

	public static BigPostPostTile getWaystonePostTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(new BlockPos(x, y, z));
		if (ret instanceof BigPostPostTile) {
			return (BigPostPostTile) ret;
		} else {
			return null;
		}
	}

	@Override
	public void clickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.sign.rot(-15, x, z);
		}
	}

	@Override
	public void rightClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.sign.rot(15, x, z);
		}
	}

	@Override
	public void shiftClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.sign.flip = !tilebases.sign.flip;
		}
	}

	@Override
	public void clickBrush(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z){
		NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostBrushID, x, y, z), player);
	}

	@Override
	public void rightClickBrush(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z){
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if(tilebases.awaitingPaint && tilebases.paintObject!=null){
			tilebases.paintObject = null;
			tilebases.awaitingPaint = false;
		}else{
			BigHit hit = (BigHit)hitObj;
			tilebases.awaitingPaint = true;
			if(hit.target == BigHitTarget.POST){
				tilebases.paintObject = tilebases;
			} else if (hit.target == BigHitTarget.BASE) {
				tilebases.paintObject = tilebases.sign;
			} else{
				tilebases.paintObject = null;
				tilebases.awaitingPaint = false;
			}
		}
	}

	public void clickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z){
		if(((BigHit)hitObj).target.equals(BigHitTarget.BASE)){
			Sign sign = ((BigPostPostTile)superTile).getBases().sign;
			if(sign != null){
				sign.rotation = (sign.flip?90:270) - (int) (player.rotationYawHead);
			}
		}
	}
	
	public void rightClickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z){
		BigHit hit = (BigHit)hitObj;
		if(hit.target.equals(BigHitTarget.BASE)){
			NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostRotationID, x, y, z), player);
		}
	}
	
	public void shiftClickCalibratedWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z){
		if(((BigHit)hitObj).target.equals(BigHitTarget.BASE)){
			Sign sign = ((BigPostPostTile)superTile).getBases().sign;
			if(sign != null){
				sign.rotation = (sign.flip?270:90) - (int) (player.rotationYawHead);
			}
		}
	}
	
	@Override
	public void click(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			if(tilebases.sign.overlay != null){
				player.inventory.addItemStackToInventory(new ItemStack(tilebases.sign.overlay.item, 1));
			}
		}
		for(OverlayType now: OverlayType.values()){
			if(player.getHeldItemMainhand().getItem().getClass() == now.item.getClass()){
				if (hit.target == BigHitTarget.BASE) {
					tilebases.sign.overlay = now;
				}
				player.inventory.clearMatchingItems(now.item, 0, 1, null);
				return;
			}
		}
		if (hit.target == BigHitTarget.BASE) {
			tilebases.sign.overlay = null;
		}
	}

	@Override
	public void rightClick(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		if (hit.target != BigHitTarget.POST) {
			if (ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
				return;
			}
			BigPostPostTile tile = (BigPostPostTile)superTile;
			BaseInfo destination = tile.getBases().sign.base;
			if (destination != null) {
				if(destination.teleportPosition == null){
					NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.noTeleport"), player);
				}else{
					int stackSize = PostHandler.getStackSize(destination.teleportPosition, tile.toPos());
					if(PostHandler.canPay(player, destination.teleportPosition.toBlockPos(), new BlockPos(x, y, z))){
						PostHandler.teleportMe(destination, player, stackSize);
					}else{
						String[] keyword = { "<itemName>", "<amount>" };
						String[] replacement = { ClientConfigStorage.INSTANCE.getCost().getTranslationKey() + ".name",	"" + stackSize };
						NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.payment", keyword, replacement), player);
					}
				}
			}
		} else {
			if(!canUse(player, superTile)) return;
			NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBigPostID, x, y, z), player);
			NetworkHandler.netWrap.sendTo(new SendAllWaystoneNamesMessage(PostHandler.getAllWaystones().select(b -> b.getName())), player);
		}
	}

	@Override
	public void shiftClick(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.sign.point = !tilebases.sign.point;
		}
	}

	@Override
	public void clickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.sign.point = !tilebases.sign.point;
		}
	}

	@Override
	public void shiftClickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayerMP player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		shiftClick(hitObj, superTile, player, x, y, z);
	}

	@Override
	public void sendPostBasesToAll(SuperPostPostTile superTile) {
		BigPostPostTile tile = (BigPostPostTile)superTile;
		BigBaseInfo tilebases = tile.getBases();
		NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage(tile, tilebases));
	}

	@Override
	public void sendPostBasesToServer(SuperPostPostTile superTile) {
		BigPostPostTile tile = (BigPostPostTile)superTile;
		BigBaseInfo tilebases = tile.getBases();
		NetworkHandler.netWrap.sendToServer(new SendBigPostBasesMessage(tile, tilebases));
	}
	
	@Override
	public Object getHitTarget(World world, int x, int y, int z, EntityPlayer player){
		DDDVector head = new DDDVector(player.posX, player.posY, player.posZ);
		head.y+=player.getEyeHeight();
		if(player.isSneaking())
			head.y-=0.08;
		Vec3d look = player.getLookVec();
		BigPostPostTile tile = getWaystonePostTile(world, x, y, z);
		BigBaseInfo bases = tile.getBases();
		DDDVector rotPos = new DDDVector(x+0.5,y+0.5,z+0.5);
		DDDVector signPos;
		DDDVector edges = new DDDVector(1.4375, 0.75, 0.0625);
		
		if(bases.sign.flip){
			signPos = new DDDVector(x-0.375, y+0.1875, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.1875, z+0.625);
		}
		Cuboid sign = new Cuboid(signPos, edges, bases.sign.calcRot(x, z), rotPos);
		Cuboid post = new Cuboid(new DDDVector(x+0.375, y, z+0.375), new DDDVector(0.25, 1, 0.25), 0);
		Cuboid waystone = new Cuboid(new DDDVector(x+0.25, y, z+0.25), new DDDVector(0.5, 0.5, 0.5), 0);

		DDDVector start = new DDDVector(head.x, head.y, head.z);
		DDDVector end = start.add(new DDDVector(look.x, look.y, look.z));
		Intersect signHit = sign.traceLine(start, end, true);
		Intersect postHit = post.traceLine(start, end, true);
		Intersect waystoneHit = waystone.traceLine(start, end, true);
		double signDist = signHit.exists&&bases.sign.base!=null&&bases.sign.base.hasName()?signHit.pos.distance(start):Double.MAX_VALUE;
		double postDist = postHit.exists?postHit.pos.distance(start):Double.MAX_VALUE/2;
		double waystoneDist = waystoneHit.exists&&tile.isWaystone()?waystoneHit.pos.distance(start):Double.MAX_VALUE;
		double dist;
		BigHitTarget target;
		DDDVector pos;
		dist = signDist;
		pos = signHit.pos;
		target = BigHitTarget.BASE;
		if(waystoneDist<dist){
			dist = waystoneDist;
			pos = waystoneHit.pos;
			target = BigHitTarget.STONE;
		}
		if(postDist<dist){
			dist = postDist;
			pos = postHit.pos;
			target = BigHitTarget.POST;
		}
		return new BigHit(target, pos);
	}

	@Override
	public Paintable getPaintableByHit(SuperPostPostTile tile, Object hit){
		switch((BigHitTarget)hit){
		case BASE:
			return ((BigPostPostTile)tile).getBases().sign;
		case POST:
			return ((BigPostPostTile)tile).getBases();
		default:
			return null;
		}
	}
	
	public static BigPostPostTile getTile(World world, BlockPos pos) {
		TileEntity ret = world.getTileEntity(pos);
		if (ret instanceof BigPostPostTile) {
			return (BigPostPostTile) ret;
		} else {
			return null;
		}
	}

	@Override
	protected boolean isHitWaystone(Object hitObj) {
		BigHit hit = (BigHit)hitObj;
		return hit.target == BigHitTarget.STONE;
	}
	
}