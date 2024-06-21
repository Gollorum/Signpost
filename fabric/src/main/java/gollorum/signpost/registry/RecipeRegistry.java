package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.crafting.CutWaystoneRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class RecipeRegistry {

	public static final CutWaystoneRecipe.Serializer CutWaystoneSerializer = new CutWaystoneRecipe.Serializer();

	public static void register(){
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, new ResourceLocation(Signpost.MOD_ID, CutWaystoneRecipe.RegistryName), CutWaystoneSerializer);
	}

}
