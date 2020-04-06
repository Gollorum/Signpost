package gollorum.signpost.worldGen.villages.waystone;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.BaseModelPost;
import gollorum.signpost.blocks.BasePost;
import gollorum.signpost.blocks.WaystoneContainer;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.worldGen.villages.GenerateStructureHelper;
import gollorum.signpost.worldGen.villages.NameLibrary;
import gollorum.signpost.worldGen.villages.VillageLibrary;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class VillageComponentWaystone extends StructureVillagePieces.Village{
	
	private boolean built = false;
	private StructureVillagePieces.Start start;
	private EnumFacing facing;
	
	public VillageComponentWaystone(){
		super();
	}
	
	public VillageComponentWaystone(StructureVillagePieces.Start start, int type, StructureBoundingBox boundingBox, EnumFacing facing){
		super(start, type);
		this.boundingBox = boundingBox;
		this.start = start;
		this.facing = facing;
	}
	
	@Nullable
	public static StructureVillagePieces.Village buildComponent(StructureVillagePieces.Start startPiece, List<StructureComponent> pieces, Random random, int x, int y, int z, EnumFacing facing, int type) {
		StructureBoundingBox boundingBox = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 1, 1, 1, facing);
		if (canVillageGoDeeper(boundingBox) && findIntersecting(pieces, boundingBox) == null) {
			return new VillageComponentWaystone(startPiece, type, boundingBox, facing.getOpposite());
		}
		return null;
	}

	@Override
	public boolean addComponentParts(final World world, Random random, StructureBoundingBox boundingBox) {
		if(built || start==null ||! NameLibrary.getInstance().namesLeft()){
			return true;
		}else{
			built = true;
		}
		final String name = NameLibrary.getInstance().getName(random);
		if(name==null){
			return true;
		}
		int x = (this.boundingBox.minX + this.boundingBox.maxX)/2;
		int z = (this.boundingBox.minZ + this.boundingBox.maxZ)/2;
		BlockPos postPos = GenerateStructureHelper.getInstance().getTopSolidOrLiquidBlock(world, new BlockPos(x, 0, z));

		List<BaseModelPost> allowedModelTypes = Signpost.proxy.blockHandler.baseModelsForVillages();
		if(allowedModelTypes.size() == 0) return true;

		if (world.getBlockState(postPos.add(0, -1, 0)).getMaterial().isLiquid()) {
			IBlockState block = this.getBiomeSpecificBlockState(Blocks.PLANKS.getDefaultState());
			world.setBlockState(postPos.add(0, -1, 0), block);
		}
		final BlockPos finalPos = postPos;
		if(world.setBlockState(finalPos, allowedModelTypes.get(random.nextInt(allowedModelTypes.size())).getStateForFacing(facing), 3)){
			SPEventHandler.scheduleTask(() -> {
				TileEntity tile = world.getTileEntity(finalPos);
				if(tile instanceof WaystoneContainer){
					if(PostHandler.getNativeWaystones().nameTaken(name)) {
						setupWaystone(NameLibrary.getInstance().getName(random), world, finalPos, (WaystoneContainer) tile);
					} else {
						setupWaystone(name, world, finalPos, (WaystoneContainer) tile);
					}
					return true;
				}else{
					return false;
				}
			});
		}
		return true;
	}

	private void setupWaystone(String name, World world, BlockPos postPos, WaystoneContainer container) {
		
		assureBaseInfo(container, world, new MyBlockPos(world, postPos), facing, name);
		
		StructureBoundingBox villageBox = start.getBoundingBox();
		MyBlockPos villagePos = new MyBlockPos(world, villageBox.minX, 0, villageBox.minZ);
		MyBlockPos blockPos = new MyBlockPos(world, postPos);
		VillageLibrary.getInstance().putWaystone(villagePos, blockPos);
	}
	
	private void assureBaseInfo(WaystoneContainer container, World world, MyBlockPos blockPos, EnumFacing facing, String name){
		if(container.getBaseInfo()==null){
			MyBlockPos telePos = GenerateStructureHelper.getInstance().getTopSolidOrLiquidBlock(blockPos.front(facing, 2));
			if (telePos.getWorld().getBlockState(telePos.toBlockPos().add(0, -1, 0)).getBlock() == Blocks.GRASS_PATH) {
				telePos.y++;
			}
			BasePost.generate(world, blockPos, telePos, name);
		} else {
			container.setName(name);
		}
	}
}
