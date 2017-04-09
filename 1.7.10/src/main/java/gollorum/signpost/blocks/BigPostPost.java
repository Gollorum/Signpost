package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
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

public class BigPostPost extends BlockContainer {

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
	public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
		if (world.isRemote||!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)) {
			return;
		}
		BigHit hit = getHitTarget(world, x, y, z, player);
		if(hit.target == BigHitTarget.POST){
			return;
		}
		BigPostPostTile tile = getTile(world, x, y, z);
		if (player.getHeldItem() != null){
			Item item = player.getHeldItem().getItem();
			if(item instanceof ItemBlock && doThingsWithItem(item, hit, tile)){
				NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage(tile, tile.getBases()));
				return;
			}
			if(item instanceof PostWrench) {
				BigBaseInfo tilebases = tile.getBases();
				if (player.isSneaking()) {
					if (hit.target == BigHitTarget.BASE) {
						tilebases.flip = !tilebases.flip;
					}
				} else {
					if (hit.target == BigHitTarget.BASE) {
						tilebases.rotation = (tilebases.rotation - 15) % 360;
					}
				}
				if(ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)) {
					NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage(tile, tilebases));
				}
			}else{
				if (player.isSneaking()) {
					BigBaseInfo tilebases = tile.getBases();
					if (hit.target == BigHitTarget.BASE) {
						tilebases.point = !tilebases.point;
					}
				}else{
					BigBaseInfo tilebases = tile.getBases();
					if (hit.target == BigHitTarget.BASE) {
						if(tilebases.overlay != null){
							player.inventory.addItemStackToInventory(new ItemStack(tilebases.overlay.item, 1));
						}
					}
					for(OverlayType now: OverlayType.values()){
						if(item.getClass() == now.item.getClass()){
							if (hit.target == BigHitTarget.BASE) {
								tilebases.overlay = now;
							}
							player.inventory.consumeInventoryItem(now.item);
							NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage(tile, tilebases));
							return;
						}
					}
					if (hit.target == BigHitTarget.BASE) {
						tilebases.overlay = null;
					}
					NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage(tile, tilebases));
				}
			}
		}else{
			BigBaseInfo tilebases = tile.getBases();
			if (hit.target == BigHitTarget.BASE) {
				tilebases.point = !tilebases.point;
				NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage(tile, tilebases));
			}
		}
	}

	protected boolean doThingsWithItem(Item item, BigHit hit, BigPostPostTile tile) {
		return false;
	}

	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(MinecraftForge.EVENT_BUS.post(new UseSignpostEvent(player, world, x, y, z))){
			return true;
		}
		if (world.isRemote) {
			return true;
		}
		if (player.getHeldItem() != null && player.getHeldItem().getItem() instanceof PostWrench) {
			if(!ConfigHandler.securityLevelSignpost.canUse((EntityPlayerMP) player)){
				return true;
			}
			BigPostPostTile tile = getTile(world, x, y, z);
			BigBaseInfo tilebases = tile.getBases();
			BigHit hit = getHitTarget(world, x, y, z, player);
			if (hit.target == BigHitTarget.BASE) {
				tilebases.rotation = (tilebases.rotation + 15) % 360;
			} else if (hit.target == BigHitTarget.POST){
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBigPostID, x, y, z), (EntityPlayerMP) player);
				return true;
			}
			NetworkHandler.netWrap.sendToAll(new SendBigPostBasesMessage(tile, tilebases));
		} else {
			BigPostPostTile tile = getTile(world, x, y, z);
			BigHit hit = getHitTarget(world, x, y, z, player);
			if (hit.target != BigHitTarget.POST) {
				if (ConfigHandler.deactivateTeleportation) {
					return true;
				}
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
		return true;
	}

	public BigHit getHitTarget(World world, int x, int y, int z, EntityPlayer/*MP*/ player){
		Vec3 head = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
		head.yCoord+=player.getEyeHeight();
		if(player.isSneaking())
			head.yCoord-=0.08;
		Vec3 look = player.getLookVec();
		BigBaseInfo bases = getWaystonePostTile(world, x, y, z).getBases();
		DDDVector rotPos = new DDDVector(x+0.5,y+0.5,z+0.5);
		DDDVector signPos;
		DDDVector edges = new DDDVector(1.4375, 0.375, 0.0625);
		
		if(bases.flip){
			signPos = new DDDVector(x-0.375, y+0.0625, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.0625, z+0.625);
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
	
	public static void placeClient(World world, BlockPos blockPos, EntityPlayer player) {
		// TODO Auto-generated method stub

	}

	public static void placeServer(World world, BlockPos blockPos, EntityPlayerMP player) {
		// TODO Auto-generated method stub

	}
}
