package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.PostPost.Hit;
import gollorum.signpost.event.UseSignpostEvent;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.BigBaseInfo.OverlayType;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.math.tracking.Cuboid;
import gollorum.signpost.util.math.tracking.DDDVector;
import gollorum.signpost.util.math.tracking.Intersect;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class BigPostPost extends SuperPostPost {

	public BigPostType type;

	public static enum BigPostType{
						OAK(	Material.wood, 	"bigsign", 			"planks_oak",		Item.getItemFromBlock(Blocks.planks),		0),
						SPRUCE(	Material.wood, 	"bigsign_spruce", 	"planks_spruce",	Item.getItemFromBlock(Blocks.planks),		1),
						BIRCH(	Material.wood, 	"bigsign_birch", 	"planks_birch",		Item.getItemFromBlock(Blocks.planks),		2),
						JUNGLE(	Material.wood,	"bigsign_jungle", 	"planks_jungle",	Item.getItemFromBlock(Blocks.planks),		3),
						ACACIA(	Material.wood, 	"bigsign_acacia", 	"planks_acacia",	Item.getItemFromBlock(Blocks.planks),		4),
						BIGOAK(	Material.wood, 	"bigsign_big_oak", 	"planks_big_oak",	Item.getItemFromBlock(Blocks.planks),		5),
						IRON(	Material.iron, 	"bigsign_iron", 	"iron_block",		Items.iron_ingot,							0),
						STONE(	Material.rock, 	"bigsign_stone", 	"stone",			Item.getItemFromBlock(Blocks.stone),		0);
		public Material material;
		public ResourceLocation texture;
		public String textureMain;
		public Item baseItem;
		public int metadata;

		private BigPostType(Material material, String texture, String textureMain, Item baseItem, int metadata) {
			this.material = material;
//			this.texture = new ResourceLocation(Signpost.MODID + ":textures/blocks/"+texture+".png");
			this.texture = new ResourceLocation("minecraft:textures/blocks/"+textureMain+".png");
			this.textureMain = textureMain;
			this.baseItem = baseItem;
			this.metadata = metadata;
		}
	}
	
	public static enum BigHitTarget{BASE, POST;}
	
	public static class BigHit{
		public BigHitTarget target;
		public DDDVector pos;
		public BigHit(BigHitTarget target, DDDVector pos){
			this.target = target; this.pos = pos;
		}
	}

	@Deprecated
	public BigPostPost() {
		super(Material.wood);
		setBlockName("SignBigPostPost");
		setCreativeTab(CreativeTabs.tabTransport);
		setBlockTextureName("minecraft:planks_oak");
		this.setHardness(2);
		this.setResistance(100000);
		float f = 15F / 32;
		this.setBlockBounds(0.5f - f, 0.0F, 0.5F - f, 0.5F + f, 1, 0.5F + f);
	}
	
	public BigPostPost(BigPostType type){
		super(type.material);
		this.type = type;
		setBlockName("SignBigPostPost"+type.toString());
		setCreativeTab(CreativeTabs.tabTransport);
		setBlockTextureName("minecraft:"+type.textureMain);
		this.setHardness(2);
		this.setResistance(100000);
		float f = 15F / 32;
		this.setBlockBounds(0.5f - f, 0.0F, 0.5F - f, 0.5F + f, 1, 0.5F + f);
	}
	
	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
		BigPostPostTile tile = new BigPostPostTile(type);
		return tile;
	}

	public static BigPostPostTile getWaystonePostTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(x, y, z);
		if (ret instanceof BigPostPostTile) {
			return (BigPostPostTile) ret;
		} else {
			return null;
		}
	}

	@Override
	public void clickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.rotation = (tilebases.rotation - 15) % 360;
		}
	}

	@Override
	public void rightClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.rotation = (tilebases.rotation + 15) % 360;
//		} else if (hit.target == BigHitTarget.POST){
//			NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBigPostID, x, y, z), (EntityPlayerMP) player);
		}
	}

	@Override
	public void shiftClickWrench(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.flip = !tilebases.flip;
		}
	}

	@Override
	public void click(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			if(tilebases.overlay != null){
				player.inventory.addItemStackToInventory(new ItemStack(tilebases.overlay.item, 1));
			}
		}
		for(OverlayType now: OverlayType.values()){
			if(player.getHeldItem().getItem().getClass() == now.item.getClass()){
				if (hit.target == BigHitTarget.BASE) {
					tilebases.overlay = now;
				}
				player.inventory.consumeInventoryItem(now.item);
				return;
			}
		}
		if (hit.target == BigHitTarget.BASE) {
			tilebases.overlay = null;
		}
	}

	@Override
	public void rightClick(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		if (hit.target != BigHitTarget.POST) {
			if (ConfigHandler.deactivateTeleportation) {
				return;
			}
			BigPostPostTile tile = (BigPostPostTile)superTile;
			BaseInfo destination = tile.getBases().base;
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
						String[] replacement = { ConfigHandler.cost.getUnlocalizedName() + ".name",	"" + stackSize };
						NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.payment", keyword, replacement), (EntityPlayerMP) player);
					}
				}
			}
		} else {
			NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBigPostID, x, y, z), (EntityPlayerMP) player);
		}
	}

	@Override
	public void shiftClick(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.point = !tilebases.point;
		}
	}

	@Override
	public void clickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		BigBaseInfo tilebases = ((BigPostPostTile)superTile).getBases();
		if (hit.target == BigHitTarget.BASE) {
			tilebases.point = !tilebases.point;
		}
	}

	@Override
	public void shiftClickBare(Object hitObj, SuperPostPostTile superTile, EntityPlayer player, int x, int y, int z) {
		BigHit hit = (BigHit)hitObj;
		shiftClick(hitObj, superTile, player, x, y, z);
	}

	@Override
	public void sendPostBases(SuperPostPostTile superTile) {
		BigPostPostTile tile = (BigPostPostTile)superTile;
		BigBaseInfo tilebases = tile.getBases();
		NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage(tile, tilebases));
	}
	
	@Override
	public Object getHitTarget(World world, int x, int y, int z, EntityPlayer player){
		Vec3 head = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
		head.yCoord+=player.getEyeHeight();
		if(player.isSneaking())
			head.yCoord-=0.08;
		Vec3 look = player.getLookVec();
		BigBaseInfo bases = getWaystonePostTile(world, x, y, z).getBases();
		DDDVector rotPos = new DDDVector(x+0.5,y+0.5,z+0.5);
		DDDVector signPos;
		DDDVector edges = new DDDVector(1.4375, 0.75, 0.0625);
		
		if(bases.flip){
			signPos = new DDDVector(x-0.375, y+0.1875, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.1875, z+0.625);
		}
		Cuboid sign = new Cuboid(signPos, edges, BigPostPostTile.calcRot(bases, x, z), rotPos);
		Cuboid post = new Cuboid(new DDDVector(x+0.375, y, z+0.375), new DDDVector(0.25, 1, 0.25), 0);

		DDDVector start = new DDDVector(head.xCoord, head.yCoord, head.zCoord);
		DDDVector end = start.add(new DDDVector(look.xCoord, look.yCoord, look.zCoord));
		Intersect signHit = sign.traceLine(start, end, true);
		Intersect postHit = post.traceLine(start, end, true);
		double signDist = signHit.exists&&bases.base!=null?signHit.pos.distance(start):Double.MAX_VALUE;
		double postDist = postHit.exists?postHit.pos.distance(start):Double.MAX_VALUE/2;
		double dist;
		BigHitTarget target;
		DDDVector pos;
		dist = signDist;
		pos = signHit.pos;
		target = BigHitTarget.BASE;
		if(postDist<dist){
			dist = postDist;
			pos = postHit.pos;
			target = BigHitTarget.POST;
		}
		return new BigHit(target, pos);
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

	public static BigPostPostTile getTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(x, y, z);
		if (ret instanceof BigPostPostTile) {
			return (BigPostPostTile) ret;
		} else {
			return null;
		}
	}
	
}
