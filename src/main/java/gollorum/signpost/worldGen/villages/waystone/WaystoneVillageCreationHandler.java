package gollorum.signpost.worldGen.villages.waystone;

import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.registry.VillagerRegistry.IVillageCreationHandler;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.worldGen.villages.NameLibrary;
import net.minecraft.world.gen.structure.StructureVillagePieces.PieceWeight;
import net.minecraft.world.gen.structure.StructureVillagePieces.Start;

public class WaystoneVillageCreationHandler implements IVillageCreationHandler{

	private static final int PIECE_WEIGHT = 3;
	private static final int COUNT = 1;
	private static final Class COMPONENT_CLASS = VillageComponentWaystone.class;

	@Override
	public PieceWeight getVillagePieceWeight(Random random, int i) {
		int postCount = 0;
		if(NameLibrary.getInstance() != null && NameLibrary.getInstance().namesLeft()){
			System.out.println(random.nextFloat()+"|"+ClientConfigStorage.INSTANCE.getVillageWaystonePropability());
			if(random.nextFloat() <= ClientConfigStorage.INSTANCE.getVillageWaystonePropability()){
				postCount = COUNT;
			}
		}
		return new PieceWeight(COMPONENT_CLASS, PIECE_WEIGHT, postCount);
	}

	@Override
	public Class<?> getComponentClass() {
		return COMPONENT_CLASS;
	}

	@Override
	public Object buildComponent(PieceWeight villagePiece, Start startPiece, List pieces, Random random, int x, int y, int z, int facing, int type) {
		return VillageComponentWaystone.buildComponent(startPiece, pieces, random, x, y, z, facing, type);
	}

}