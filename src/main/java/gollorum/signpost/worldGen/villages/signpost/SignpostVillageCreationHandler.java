package gollorum.signpost.worldGen.villages.signpost;

import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.registry.VillagerRegistry.IVillageCreationHandler;
import gollorum.signpost.management.ClientConfigStorage;
import net.minecraft.world.gen.structure.StructureVillagePieces.PieceWeight;
import net.minecraft.world.gen.structure.StructureVillagePieces.Start;

public class SignpostVillageCreationHandler implements IVillageCreationHandler{

	private static final int PIECE_WEIGHT = 3;
	private static final Class COMPONENT_CLASS = VillageComponentSignpost.class;

	@Override
	public PieceWeight getVillagePieceWeight(Random random, int i) {
		return new PieceWeight(COMPONENT_CLASS, PIECE_WEIGHT, getCount(random));
	}
	
	private int getCount(Random random){
		int minCount = ClientConfigStorage.INSTANCE.getVillageMinSignposts();
		int maxCount = ClientConfigStorage.INSTANCE.getVillageMaxSignposts();
		int offset = random.nextInt(maxCount - minCount + 1);
		return minCount + offset;
	}

	@Override
	public Class<?> getComponentClass() {
		return COMPONENT_CLASS;
	}

	@Override
	public Object buildComponent(PieceWeight villagePiece, Start startPiece, List pieces, Random random, int x, int y, int z, int facing, int type) {
		return VillageComponentSignpost.buildComponent(startPiece, pieces, random, x, y, z, facing, type);
	}

}