package gollorum.signpost.worldGen.villages.signpost;

import java.util.List;
import java.util.Random;

import gollorum.signpost.management.ClientConfigStorage;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.gen.structure.StructureVillagePieces.PieceWeight;
import net.minecraft.world.gen.structure.StructureVillagePieces.Start;
import net.minecraft.world.gen.structure.StructureVillagePieces.Village;
import net.minecraftforge.fml.common.registry.VillagerRegistry.IVillageCreationHandler;

public class SignpostVillageCreationHandler implements IVillageCreationHandler{

	private static final Class COMPONENT_CLASS = VillageComponentSignpost.class;

	@Override
	public PieceWeight getVillagePieceWeight(Random random, int i) {
		if (ClientConfigStorage.INSTANCE.isDisableVillageGeneration()) {
			return new PieceWeight(COMPONENT_CLASS, 0, 0);
		}
		return new PieceWeight(COMPONENT_CLASS, ClientConfigStorage.INSTANCE.getVillageSignpostsWeight(), getCount(random));
	}

	private int getCount(Random random) {
		return ClientConfigStorage.INSTANCE.getVillageMaxSignposts();
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