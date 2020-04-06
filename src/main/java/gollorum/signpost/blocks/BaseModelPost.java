package gollorum.signpost.blocks;

import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.tiles.BasePostTile;
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
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
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

		private ModelType(int i){
			this(allTypeIds[i], allTypeNames[i]);
		}

		private ModelType(int ID, String name){
			this.ID = ID;
			this.name = name;
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

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public final ModelType type;
	
	public BaseModelPost(int typ) {
		super(Material.ROCK);
		this.setHarvestLevel("pickaxe", 1);
		this.setHardness(2);
		this.setResistance(100000);
		setCreativeTab(CreativeTabs.TRANSPORTATION);
		this.setTranslationKey("SignpostBase");
		type = ModelType.values()[typ];
		this.setRegistryName(Signpost.MODID+":blockbasemodel"+type.getID());
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.SOUTH));
	}

	@Override
	protected BlockStateContainer createBlockState() {
	    return new BlockStateContainer(this, new IProperty[] { FACING });
	}

	@Override
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer){
		return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	} 
	 
	public IBlockState getStateForFacing(EnumFacing facing) {
		return this.getDefaultState().withProperty(FACING, facing);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex();
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
	    return new ItemStack(Item.getItemFromBlock(this), 1, this.getMetaFromState(world.getBlockState(pos)));
	}
	
	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (ClientConfigStorage.INSTANCE.deactivateTeleportation()) {
			return false;
		}
		if (!worldIn.isRemote) {
			BaseInfo ws = getWaystoneRootTile(worldIn, pos).getBaseInfo();
			if(ws==null){
				ws = new BaseInfo(BasePost.generateName(), new MyBlockPos(pos, playerIn.dimension), playerIn.getUniqueID());
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
					NetworkHandler.netWrap.sendTo(new OpenGuiMessage(Signpost.GuiBaseID, pos.getX(), pos.getY(), pos.getZ()), (EntityPlayerMP) playerIn);
				}
			}
		}
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new BasePostTile().setup();
	}

	public static BasePostTile getWaystoneRootTile(World world, BlockPos pos) {
		TileEntity ret = world.getTileEntity(pos);
		if (ret instanceof BasePostTile) {
			return (BasePostTile) ret;
		} else {
			return null;
		}
	}

	public static void placeServer(World world, MyBlockPos blockPos, EntityPlayerMP player) {
		MyBlockPos telePos = new MyBlockPos(player);
		BasePostTile tile = getWaystoneRootTile(world, blockPos.toBlockPos());
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

	public static void placeClient(final World world, final MyBlockPos pos, final EntityPlayer player) {
//		BasePostTile tile = getWaystoneRootTile(world, pos.toBlockPos());
//		if (tile != null && tile.getBaseInfo() == null) {
//			BaseInfo ws = PostHandler.allWaystones.getByPos(pos);
//			if (ws == null) {
//				UUID owner = player.getUniqueID();
//				PostHandler.allWaystones.add(new BaseInfo("", pos, owner));
//			}
//		}
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		BasePostTile ret = new BasePostTile();
//		if(!worldIn.isRemote){
//			ret.setup();
//		}
		return ret;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}

	public static BaseModelPost[] createAll() {
		BaseModelPost[] ret = new BaseModelPost[ModelType.values().length];
		for(int i=0; i<ModelType.values().length; i++){
			ret[i] = new BaseModelPost(i);
		}
		return ret;
	}

}
