package gollorum.signpost.worldGen.villages;

import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.worldGen.villages.signpost.SignpostVillageCreationHandler;
import gollorum.signpost.worldGen.villages.signpost.VillageComponentSignpost;
import gollorum.signpost.worldGen.villages.waystone.VillageComponentWaystone;
import gollorum.signpost.worldGen.villages.waystone.WaystoneVillageCreationHandler;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

public class VillageHandler {
	
	private static final VillageHandler INSTANCE = new VillageHandler();
	
	public static VillageHandler getInstance(){
		return INSTANCE;
	}
	
	private VillageHandler(){}

	public void register(){
		if (ClientConfigStorage.INSTANCE.isDisableVillageGeneration()) {
			return;
		}
		registerVillagePieces();
		registerHandlers();
	}

	private void registerVillagePieces(){
		registerVillagePiece("villageComponentSignpost", VillageComponentSignpost.class);
		registerVillagePiece("villageComponentWaystone", VillageComponentWaystone.class);
	}

	private void registerVillagePiece(String name, Class<? extends StructureComponent> componentClass){
		MapGenStructureIO.registerStructureComponent(componentClass, name);
	}
	
	private void registerHandlers() {
		VillagerRegistry.instance().registerVillageCreationHandler(new SignpostVillageCreationHandler());
		VillagerRegistry.instance().registerVillageCreationHandler(new WaystoneVillageCreationHandler());
	}
}