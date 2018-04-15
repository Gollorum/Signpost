package gollorum.signpost.worldGen.villages.signpost;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import gollorum.signpost.Signpost;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.worldGen.villages.GenerateStructureHelper;
import gollorum.signpost.worldGen.villages.VillageLibrary;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

public class VillageComponentSignpost extends StructureVillagePieces.Village{
	
	private boolean built = false;
	private StructureVillagePieces.Start start;
	private int facing;
	
	public VillageComponentSignpost(){
		super();
	}
		
	public VillageComponentSignpost(StructureVillagePieces.Start start, int type, StructureBoundingBox boundingBox, int facing){
		super(start, type);
		this.boundingBox = boundingBox;
		this.start = start;
		this.facing = facing;
	}
	
	@Nullable
	public static StructureVillagePieces.Village buildComponent(StructureVillagePieces.Start startPiece, List<StructureComponent> pieces, Random random, int x, int y, int z, int facing, int type) {
		StructureBoundingBox boundingBox = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 3, 3, 3, facing);
		if (canVillageGoDeeper(boundingBox) && findIntersecting(pieces, boundingBox) == null) {
			return new VillageComponentSignpost(startPiece, type, boundingBox, facing);
		}
		return null;
	}

	@Override
	public boolean addComponentParts(World world, Random random, StructureBoundingBox boundingBox) {
		if(built || start==null){
			return true;
		}else{
			built = true;
		}
		int x = this.boundingBox.getCenterX();
		int z = this.boundingBox.getCenterZ();
		int y = GenerateStructureHelper.getInstance().getTopSolidOrLiquidBlock(world, x, z);
		world.setBlock(x, y, z, Signpost.proxy.blockHandler.post_oak);
		world.setBlock(x, y + 1, z, Signpost.proxy.blockHandler.post_oak);
		StructureBoundingBox villageBox = start.getBoundingBox();
		MyBlockPos villagePos = new MyBlockPos(world, villageBox.minX, 0, villageBox.minZ);
		MyBlockPos blockPos = new MyBlockPos(world, x, y + 1, z);
		VillageLibrary.getInstance().putSignpost(villagePos, blockPos, optimalRot(facing));
		return true;
	}

	private double optimalRot(int facing) {
		switch(facing){
			case 0:
				return 0;
			case 1:
				return 1.5*Math.PI;
			case 2:
				return Math.PI;
			case 3:
				return 0.5*Math.PI;
		}
		return 0;
	}
}
