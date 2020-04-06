package gollorum.signpost.blocks;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import gollorum.signpost.SPEventHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.BaseModelPostTile;
import gollorum.signpost.event.UpdateWaystoneEvent;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.BaseUpdateClientMessage;
import gollorum.signpost.network.messages.ChatMessage;
import gollorum.signpost.network.messages.OpenGuiMessage;
import gollorum.signpost.util.BaseInfo;
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

import java.util.UUID;

public class BaseModelPost extends BlockContainer {

	public static final String[] allTypeNames = {"simple0", "simple1", "simple2", "detailed0", "detailed1", "aer", "dwarf", "ygnar"};
	public static final String[] allDefaultVillageTypeNames = {"simple0", "simple1", "simple2", "detailed0", "detailed1"};
    public static final int[] allTypeIds = {5, 6, 7, 0, 1, 2, 3, 4};

	public static enum ModelType {

        MODEL0(0),
		MODEL1(1),
		MODEL2(2),
		MODEL3(3),
		MODEL4(4),
        MODEL5(5),
        MODEL6(6),
        MODEL7(7);

		private int ID;
        public final String name;
		public IModelCustom MODEL;
		public final ResourceLocation TEXTURE;

        private ModelType(int i){
            this(allTypeIds[i], allTypeNames[i]);
        }

        private ModelType(int ID, String name){
            this(ID, name, new ResourceLocation("signpost:models/block/"+name+".obj"), new ResourceLocation("signpost:textures/blocks/waystone.png"));
        }

		private ModelType(int ID, String name, final ResourceLocation model, ResourceLocation texture){
			this.ID = ID;
			this.name = name;
			MODEL = null;
			if(FMLCommonHandler.instance().getEffectiveSide().equals(Side.CLIENT) || FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()){
				SPEventHandler.scheduleTask(() -> {
					try{
						MODEL = AdvancedModelLoader.loadModel(model);
						return true;
					}catch(Exception e){
						e.printStackTrace();
						return false;
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

		public static ModelType getByID(int ID){
            for(ModelType now: ModelType.values()){
                if(ID == now.ID){
                    return now;
                }
            }
            return ModelType.MODEL1;
        }

        public static ModelType getByName(String name){
            for(ModelType now: ModelType.values()){
                if(name.equals(now.name)){
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
				ws = new BaseInfo(BasePost.generateName(), new MyBlockPos(x, y, z, playerIn.dimension), playerIn.getUniqueID());
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
	}

	public static void placeClient(final World world, final MyBlockPos blockPos, final EntityPlayer player) {}

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
