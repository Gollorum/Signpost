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
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class VillageComponentWaystone extends StructureVillagePieces.Village {

	private boolean built = false;
	private StructureVillagePieces.Start start;
	private int facing;

	public VillageComponentWaystone() {
		super();
	}

	public VillageComponentWaystone(StructureVillagePieces.Start start, int type, StructureBoundingBox boundingBox,
			int facing) {
		super(start, type);
		this.boundingBox = boundingBox;
		this.start = start;
		this.facing = facing;
	}

	@Nullable
	public static StructureVillagePieces.Village buildComponent(StructureVillagePieces.Start startPiece,
			List<StructureComponent> pieces, Random random, int x, int y, int z, int facing, int type) {
		StructureBoundingBox boundingBox = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 1, 1, 1,
				facing);
		if (canVillageGoDeeper(boundingBox) && findIntersecting(pieces, boundingBox) == null) {
			return new VillageComponentWaystone(startPiece, type, boundingBox, facing);
		}
		return null;
	}

	@Override
	public boolean addComponentParts(final World world, final Random random, StructureBoundingBox boundingBox) {
		if (built || start == null || !NameLibrary.getInstance().namesLeft()) {
			return true;
		} else {
			built = true;
		}
		final String name = NameLibrary.getInstance().getName(random);
		if (name == null) {
			return true;
		}
		final int x = this.boundingBox.getCenterX();
		final int z = this.boundingBox.getCenterZ();
		final int y = GenerateStructureHelper.getInstance().getTopSolidOrLiquidBlock(world, x, z);

		List<BaseModelPost> allowedModelTypes = Signpost.proxy.blockHandler.baseModelsForVillages();
		if(allowedModelTypes.size() == 0) return true;

		if (world.getBlock(x, y - 1, z).getMaterial().isLiquid()) {
			Block block = this.func_151558_b(Blocks.planks, 0);
			world.setBlock(x, y - 1, z, block);
		}
		if (world.setBlock(x, y, z, allowedModelTypes.get(random.nextInt(allowedModelTypes.size())), facing, 3)) {
			SPEventHandler.scheduleTask(() -> {
				TileEntity tile = world.getTileEntity(x, y, z);
				if (tile instanceof WaystoneContainer) {
					if (PostHandler.getNativeWaystones().nameTaken(name)) {
						setupWaystone(NameLibrary.getInstance().getName(random), world, x, y, z, (WaystoneContainer) tile);
					} else {
						setupWaystone(name, world, x, y, z, (WaystoneContainer) tile);
					}
					return true;
				} else {
					return false;
				}
			});
		}
		return true;
	}

	private void setupWaystone(String name, World world, int x, int y, int z, WaystoneContainer container) {
		assureBaseInfo(container, world, new MyBlockPos(world, x, y, z), getEnumFacing(facing), name);

		StructureBoundingBox villageBox = start.getBoundingBox();
		MyBlockPos villagePos = new MyBlockPos(world, villageBox.minX, 0, villageBox.minZ);
		MyBlockPos blockPos = new MyBlockPos(world, x, y, z);
		VillageLibrary.getInstance().putWaystone(villagePos, blockPos);
	}

	private void assureBaseInfo(WaystoneContainer container, World world, MyBlockPos blockPos, EnumFacing facing,
			String name) {
		if (container.getBaseInfo() == null) {
			MyBlockPos telePos = GenerateStructureHelper.getInstance()
					.getTopSolidOrLiquidBlock(blockPos.front(facing, 2));
			BasePost.generate(world, blockPos, telePos, name);
		} else {
			container.setName(name);
		}
	}

	private EnumFacing getEnumFacing(int facing) {
		switch (facing) {
		case 0:
			return EnumFacing.NORTH;
		case 1:
			return EnumFacing.WEST;
		case 2:
			return EnumFacing.SOUTH;
		case 3:
			return EnumFacing.EAST;
		}
		return EnumFacing.EAST;
	}
}
