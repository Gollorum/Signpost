package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.crafting.CutWaystoneRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class RecipeRegistry {

	public static final RecipeSerializer<CutWaystoneRecipe> CutWaystoneSerializer = CutWaystoneRecipe.SERIALIZER;

	public static void register(){
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, CutWaystoneRecipe.RegistryName), CutWaystoneSerializer);
	}

}
