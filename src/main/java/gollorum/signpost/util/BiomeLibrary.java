package gollorum.signpost.util;

import java.util.HashMap;
import java.util.Map;

import gollorum.signpost.util.code.MinecraftDependent;
import net.minecraft.world.biome.Biome;

@MinecraftDependent
public abstract class BiomeLibrary {
	
	protected Map<String, String> entries;
	
	private BiomeLibrary(){
		entries = new HashMap<String, String>();
	}

	public String getName(Biome biome){
		return getName(biome.getRegistryName().toString());
	}
	
	public String getName(String biomeRegistryName){
		return entries.get(biomeRegistryName);
	}
	
	private static final BiomeLibrary INSTANCE = new BiomeLibrary(){
		{
			entries.put("ocean", "Ocean");
			entries.put("plains", "Plains");
			entries.put("desert", "Desert");
			entries.put("extreme_hills", "Hills");
			entries.put("forest", "Forest");
			entries.put("taiga", "Taiga");
			entries.put("swampland", "Swamp");
			entries.put("river", "River");
			entries.put("hell", "Nether");
			entries.put("sky", "End");
			entries.put("frozen_ocean", "Ocean");
			entries.put("frozen_river", "River");
			entries.put("ice_flats", "Snow");
			entries.put("ice_mountains", "Snow");
			entries.put("mushroom_island", "Mushroom");
			entries.put("mushroom_island_shore", "Mushroom");
			entries.put("beaches", "Beaches");
			entries.put("desert_hills", "Desert");
			entries.put("forest_hills", "Forest");
			entries.put("taiga_hills", "Taiga");
			entries.put("smaller_extreme_hills", "Hills");
			entries.put("jungle", "Jungle");
			entries.put("jungle_hills", "Jungle");
			entries.put("jungle_edge", "Jungle");
			entries.put("deep_ocean", "Ocean");
			entries.put("stone_beach", "Beaches");
			entries.put("cold_beach", "Beaches");
			entries.put("birch_forest", "BirchForest");
			entries.put("birch_forest_hills", "BirchForest");
			entries.put("roofed_forest", "RoofedForest");
			entries.put("taiga_cold", "Taiga");
			entries.put("taiga_cold_hills", "Taiga");
			entries.put("redwood_taiga", "Taiga");
			entries.put("redwood_taiga_hills", "Taiga");
			entries.put("extreme_hills_with_trees", "Forest");
			entries.put("savanna", "Savanna");
			entries.put("savanna_rock", "Savanna");
			entries.put("mesa", "Mesa");
			entries.put("mesa_rock", "Mesa");
			entries.put("mesa_clear_rock", "Mesa");
			entries.put("void", "Void");
			entries.put("mutated_plains", "Plains");
			entries.put("mutated_desert", "Desert");
			entries.put("mutated_extreme_hills", "Hills");
			entries.put("mutated_forest", "Forest");
			entries.put("mutated_taiga", "Taiga");
			entries.put("mutated_swampland", "Swamp");
			entries.put("mutated_ice_flats", "Snow");
			entries.put("mutated_jungle", "Jungle");
			entries.put("mutated_jungle_edge", "Jungle");
			entries.put("mutated_birch_forest", "BirchForest");
			entries.put("mutated_birch_forest_hills", "BirchForest");
			entries.put("mutated_roofed_forest", "RoofedForest");
			entries.put("mutated_taiga_cold", "Snow");
			entries.put("mutated_redwood_taiga", "Taiga");
			entries.put("mutated_redwood_taiga_hills", "Taiga");
			entries.put("mutated_extreme_hills_with_trees", "Forest");
			entries.put("mutated_savanna", "Savanna");
			entries.put("mutated_savanna_rock", "Savanna");
			entries.put("mutated_mesa", "Mesa");
			entries.put("mutated_mesa_rock", "Mesa");
			entries.put("mutated_mesa_clear_rock", "Mesa");
		}
	};

	public static BiomeLibrary getInstance(){
		return INSTANCE;
	}
}
