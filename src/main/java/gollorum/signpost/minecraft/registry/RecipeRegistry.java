package gollorum.signpost.minecraft.registry;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.crafting.CycleWaystoneModelRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RecipeRegistry {

	private static final DeferredRegister<IRecipeSerializer<?>> Register =
		DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Signpost.MOD_ID);

	public static final RegistryObject<SpecialRecipeSerializer<CycleWaystoneModelRecipe>> CycleWaystoneModelSerializer =
		Register.register(
			CycleWaystoneModelRecipe.RegistryName,
			() -> new SpecialRecipeSerializer<>(CycleWaystoneModelRecipe::new)
		);

	public static void register(IEventBus bus){
		Register.register(bus);
	}

}
