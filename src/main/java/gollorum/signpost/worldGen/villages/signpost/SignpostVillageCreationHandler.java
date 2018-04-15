package gollorum.signpost.worldGen.villages.signpost;

import java.util.List;
import java.util.Random;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.gen.structure.StructureVillagePieces.PieceWeight;
import net.minecraft.world.gen.structure.StructureVillagePieces.Start;
import net.minecraft.world.gen.structure.StructureVillagePieces.Village;
import net.minecraftforge.fml.common.registry.VillagerRegistry.IVillageCreationHandler;

public class SignpostVillageCreationHandler implements IVillageCreationHandler{

	private static final int PIECE_WEIGHT = 3;
	private static final int COUNT = 1;
	private static final Class COMPONENT_CLASS = VillageComponentSignpost.class;

	@Override
	public PieceWeight getVillagePieceWeight(Random random, int i) {
		return new PieceWeight(COMPONENT_CLASS, PIECE_WEIGHT, COUNT);
	}

	@Override
	public Class<?> getComponentClass() {
		return COMPONENT_CLASS;
	}

	@Override
	public Village buildComponent(PieceWeight villagePiece, Start startPiece, List pieces, Random random, int x, int y, int z, EnumFacing facing, int type) {
		return VillageComponentSignpost.buildComponent(startPiece, pieces, random, x, y, z, facing, type);
	}

}