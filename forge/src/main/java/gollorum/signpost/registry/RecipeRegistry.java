package gollorum.signpost.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.crafting.CutWaystoneRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeRegistry {

	private static final DeferredRegister<RecipeSerializer<?>> Register =
		DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Signpost.MOD_ID);

	public static final RegistryObject<RecipeSerializer<CutWaystoneRecipe>> CutWaystoneSerializer =
		Register.register(
			CutWaystoneRecipe.RegistryName,
			() -> CutWaystoneRecipe.SERIALIZER
		);

	public static void register(BusGroup bus){
		Register.register(bus);
	}

}
