package gollorum.signpost.blocks;

import java.util.UUID;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.SPEventHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.BaseModelPostTile;
import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.MyBlockPos;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.MinecraftForge;

public class BaseModelPost extends BlockContainer {

	public static enum ModelType{
		MODEL1(0, "model0", new ResourceLocation("signpost:models/block/ws1tri.obj"), new ResourceLocation("signpost:textures/blocks/base.png")),
		MODEL2(1, "model1", new ResourceLocation("signpost:models/block/ws2tri.obj"), new ResourceLocation("signpost:textures/blocks/base.png")),
		MODEL3(2, "model2", new ResourceLocation("signpost:models/block/ws3tri.obj"), new ResourceLocation("signpost:textures/blocks/base.png")),
		MODEL4(3, "model3", new ResourceLocation("signpost:models/block/ws4tri.obj"), new ResourceLocation("signpost:textures/blocks/base.png")),
		MODEL5(4, "model4", new ResourceLocation("signpost:models/block/ws5tri.obj"), new ResourceLocation("signpost:textures/blocks/base.png"));

		private int ID;
		private String name;
		public IModelCustom MODEL;
		public final ResourceLocation TEXTURE;
			
		private ModelType(int ID, String name, final ResourceLocation model, ResourceLocation texture){
			this.ID = ID;
			this.name = name;
			MODEL = null;
			if(FMLCommonHandler.instance().getEffectiveSide().equals(Side.CLIENT) || FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()){
				SPEventHandler.scheduleTask(new BoolRun(){

					@Override
					public boolean run() {
						try{
							MODEL = AdvancedModelLoader.loadModel(model);
							return true;
						}catch(Exception e){
							e.printStackTrace();
							return false;
						}
					}
					
				});
			}
			TEXTURE = texture;
		}
		
		@Override
		public String toString(){
			return name;
		}

		public int getID(){
			return ID;
		}

		private static ModelType getByID(int ID){
			for(ModelType now: ModelType.values()){
				if(ID == now.ID){
					return now;
				}
			}
			return ModelType.MODEL1;
		}
	}

	public final ModelType type;

	public BaseModelPost(int typ) {
		super(Material.rock);
		this.setHardness(2);
		this.setResistance(100000);
		setBlockName("SignpostBase");
		setCreativeTab(CreativeTabs.tabTransport);
		setBlockTextureName(Signpost.MODID + ":base");
		type = ModelType.values()[typ];
	}
	
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack){
		int l = MathHelper.floor_double((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		world.setBlockMetadataWithNotify(x, y, z, l, 2);
	}

	@Override
	public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer playerIn, int side, float hitX, float hitY, float hitZ) {
		if (ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
			return false;
		}
		if (!worldIn.isRemote) {
			BaseInfo ws = getWaystoneRootTile(worldIn, x, y, z).getBaseInfo();
			if(ws==null){
				ws = new BaseInfo(BasePost.generateName(), new MyBlockPos(worldIn, x, y, z, playerIn.dimension), playerIn.getUniqueID());
				PostHandler.addWaystone(ws);
			}
			if (!playerIn.isSneaking()) {
				if(!PostHandler.doesPlayerKnowNativeWaystone((EntityPlayerMP) playerIn, ws)){
					if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
						NetworkHandler.netWrap.sendTo(new ChatMessage("signpost.discovered", "<Waystone>", ws.getName()), (EntityPlayerMP) playerIn);
					}
					PostHandler.addDiscovered(playerIn.getUniqueID(), ws);
				}
			} else {
				if (!ClientConfigStorage.INSTANCE.deactivateTeleportation()
						&& ClientConfigStorage.INSTANCE.getSecurityLevelWaystone().canUse((EntityPlayerMP) playerIn, ""+ws.owner)) {
					NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, x, y, z), (EntityPlayerMP) playerIn);
				}
			}
		}
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int p_149915_2_) {
		return new BaseModelPostTile(type).setup();
	}

	public static BaseModelPostTile getWaystoneRootTile(World world, int x, int y, int z) {
		TileEntity ret = world.getTileEntity(x, y, z);
		if (ret instanceof BaseModelPostTile) {
			return (BaseModelPostTile) ret;
		} else {
			return null;
		}
	}

	public static void placeServer(World world, MyBlockPos blockPos, EntityPlayerMP player) {
		MyBlockPos telePos = new MyBlockPos(player);
		BaseModelPostTile tile = getWaystoneRootTile(world, blockPos.x, blockPos.y, blockPos.z);
		String name = BasePost.generateName();
		UUID owner = player.getUniqueID();
		BaseInfo ws;
		if((ws = tile.getBaseInfo())==null){
			ws = new BaseInfo(name, blockPos, telePos, owner);
			PostHandler.addWaystone(ws);
		}else{
			ws.setAll(new BaseInfo(name, blockPos, telePos, owner));
		}
		PostHandler.addDiscovered(player.getUniqueID(), ws);
		NetworkHandler.netWrap.sendToAll(new BaseUpdateClientMessage());
		MinecraftForge.EVENT_BUS.post(new UpdateWaystoneEvent(UpdateWaystoneEvent.WaystoneEventType.PLACED, world, blockPos.x, blockPos.y, blockPos.z, name));
		NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, blockPos.x, blockPos.y, blockPos.z), player);
//		int l = MathHelper.floor_double((double)(player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
//		world.setBlockMetadataWithNotify(blockPos.x, blockPos.y, blockPos.z, l, 2);
	}

	public static void placeClient(final World world, final MyBlockPos blockPos, final EntityPlayer player) {
//		int l = MathHelper.floor_double((double)(player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
//		world.setBlockMetadataWithNotify(blockPos.x, blockPos.y, blockPos.z, l, 2);
//		BaseModelPostTile tile = getWaystoneRootTile(world, pos.toBlockPos());       
//		if (tile != null && tile.getBaseInfo() == null) {
//			BaseInfo ws = PostHandler.allWaystones.getByPos(pos);
//			if (ws == null) {
//				UUID owner = player.getUniqueID();
//				PostHandler.allWaystones.add(new BaseInfo("", pos, owner));
//			}
//	}
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

	public static BaseModelPost[] createAll() {
		BaseModelPost[] ret = new BaseModelPost[ModelType.values().length];
		for(int i=0; i<ModelType.values().length; i++){
			ret[i] = new BaseModelPost(i);
		}
		return ret;
	}
}
