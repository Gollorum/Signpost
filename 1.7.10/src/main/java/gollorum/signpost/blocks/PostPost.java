package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.items.PostWrench;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.render.PostRenderer;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.DoubleBaseInfo.OverlayType;
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
		super(Material.wood);
		setBlockName("SignpostPost");
		setCreativeTab(CreativeTabs.tabTransport);
		setBlockTextureName("minecraft:planks_oak");
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
		setBlockTextureName("minecraft:"+type.textureMain);
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
		Hit hit = getHitTarget(world, x, y, z, player);
		if(hit.target == HitTarget.POST){
			return;
		}
		PostPostTile tile = getTile(world, x, y, z);
		if (player.getHeldItem() != null){
			Item item = player.getHeldItem().getItem();
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
							player.inventory.consumeInventoryItem(now.item);
							return;
						}
					}
					if (hit.target == HitTarget.BASE1) {
						tilebases.overlay1 = null;
					}else{
						tilebases.overlay2 = null;
					}
				}
			}
		}else{
			DoubleBaseInfo tilebases = tile.getBases();
			if (hit.target == HitTarget.BASE1) {
				if(tilebases.overlay1 != null){
					player.inventory.addItemStackToInventory(new ItemStack(tilebases.overlay1.item, 1));
					tilebases.overlay1 = null;
				}
			}else{
				if(tilebases.overlay2 != null){
					player.inventory.addItemStackToInventory(new ItemStack(tilebases.overlay2.item, 1));
					tilebases.overlay2 = null;
				}
			}
		}
	}

	protected boolean doThingsWithItem(Item item, Hit hit, PostPostTile tile) {
		return false;
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
			Hit hit = getHitTarget(world, x, y, z, player);
			if (hit.target == HitTarget.BASE1) {
				tilebases.rotation1 = (tilebases.rotation1 + 15) % 360;
			} else if (hit.target == HitTarget.BASE2) {
				tilebases.rotation2 = (tilebases.rotation2 + 15) % 360;
			} else if (hit.target == HitTarget.POST){
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostID, x, y, z), (EntityPlayerMP) player);
				return true;
			}
			NetworkHandler.netWrap.sendToAll(new SendPostBasesMessage(tile, tilebases));
		} else {
			PostPostTile tile = getTile(world, x, y, z);
			Hit hit = getHitTarget(world, x, y, z, player);
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
				NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiPostID, x, y, z), (EntityPlayerMP) player);
			}
		}
		return true;
	}

	public Hit getHitTarget(World world, int x, int y, int z, EntityPlayer/*MP*/ player){
		Vec3 head = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
		head.yCoord+=player.getEyeHeight();
		if(player.isSneaking())
			head.yCoord-=0.08;
		Vec3 look = player.getLookVec();
		DoubleBaseInfo bases = getWaystonePostTile(world, x, y, z).getBases();
		DDDVector rotPos = new DDDVector(x+0.5,y+0.5,z+0.5);
		DDDVector signPos;
		DDDVector edges = new DDDVector(1.4375, 0.375, 0.0625);
		
		if(!bases.flip1){
			signPos = new DDDVector(x-0.375, y+0.5625, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.5625, z+0.625);
		}
		Cuboid sign1 = new Cuboid(signPos, edges, PostRenderer.calcRot1(bases, x, z), rotPos);
		
		if(!bases.flip2){
			signPos = new DDDVector(x-0.375, y+0.0625, z+0.625);
		}else{
			signPos = new DDDVector(x-0.0625, y+0.0625, z+0.625);
		}
		Cuboid sign2 = new Cuboid(signPos, edges, PostRenderer.calcRot2(bases, x, z), rotPos);
		Cuboid post = new Cuboid(new DDDVector(x+0.375, y, z+0.375), new DDDVector(0.25, 1, 0.25), 0);

		DDDVector start = new DDDVector(head.xCoord, head.yCoord, head.zCoord);
		DDDVector end = start.add(new DDDVector(look.xCoord, look.yCoord, look.zCoord));
		Intersect sign1Hit = sign1.traceLine(start, end, true);
		Intersect sign2Hit = sign2.traceLine(start, end, true);
		Intersect postHit = post.traceLine(start, end, true);
		double sign1Dist = sign1Hit.exists&&bases.base1!=null?sign1Hit.pos.distance(start):Double.MAX_VALUE;
		double sign2Dist = sign2Hit.exists&&bases.base2!=null?sign2Hit.pos.distance(start):Double.MAX_VALUE;
		double postDist = postHit.exists?postHit.pos.distance(start):Double.MAX_VALUE/2;
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
		if(postDist<dist){
			dist = postDist;
			pos = postHit.pos;
			target = HitTarget.POST;
		}
		return new Hit(target, pos);
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
