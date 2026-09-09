package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.crafting.CutWaystoneRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RecipeRegistry {

	private static final DeferredRegister<RecipeSerializer<?>> Register =
		DeferredRegister.create(Registries.RECIPE_SERIALIZER, Signpost.MOD_ID);

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CutWaystoneRecipe>> CutWaystoneSerializer =
		Register.register(
			CutWaystoneRecipe.RegistryName,
			() -> CutWaystoneRecipe.SERIALIZER
		);

	public static void register(IEventBus bus){
		Register.register(bus);
	}

}
