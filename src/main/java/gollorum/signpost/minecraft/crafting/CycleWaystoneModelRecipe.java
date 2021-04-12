package gollorum.signpost.minecraft.crafting;

import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.Waystone;
import gollorum.signpost.minecraft.registry.RecipeRegistry;
import net.minecraft.block.Block;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.List;
import java.util.stream.Collectors;

public class CycleWaystoneModelRecipe extends SpecialRecipe {

	public static final String RegistryName = "cycle_waystone_model";

	public CycleWaystoneModelRecipe(ResourceLocation idIn) {
		super(idIn);
	}

	@Override
	public boolean matches(CraftingInventory inv, World world) {
		ItemStack stack = getSingleItemIn(inv);
		if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return false;
		Block block = ((BlockItem) stack.getItem()).getBlock();
		return block instanceof Waystone || block instanceof ModelWaystone;
	}

	@Override
	public ItemStack getCraftingResult(CraftingInventory inv) {
		List<ModelWaystone.Variant> allowedVariants = ModelWaystone.variants.stream()
			.filter(v -> Config.Server.allowedWaystones.get().contains(v.name))
			.collect(Collectors.toList());
		ItemStack stack = getSingleItemIn(inv);
		if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return ItemStack.EMPTY;
		Block block = ((BlockItem) stack.getItem()).getBlock();
		if(!(block instanceof Waystone || block instanceof ModelWaystone)) return ItemStack.EMPTY;

		if(allowedVariants.isEmpty()) return new ItemStack(Waystone.INSTANCE);
		if(block instanceof Waystone) return new ItemStack(allowedVariants.get(0).block);
		int i = allowedVariants.indexOf(((ModelWaystone) block).variant);
		if(i < 0 || i >= allowedVariants.size() - 1) return new ItemStack(Waystone.INSTANCE);
		return new ItemStack(allowedVariants.get(i + 1).block);
	}

	private ItemStack getSingleItemIn(CraftingInventory inv) {
		ItemStack ret = ItemStack.EMPTY;
		for(int i = 0; i < inv.getSizeInventory(); ++i)
			if(!inv.getStackInSlot(i).isEmpty())
				if(ret.isEmpty()) ret = inv.getStackInSlot(i);
				else return ItemStack.EMPTY;
		return ret;
	}

	@Override
	public boolean canFit(int width, int height) {
		return width > 0 && height > 0;
	}

	@Override
	public String getGroup() {
		return Waystone.REGISTRY_NAME;
	}

	@Override
	public ItemStack getIcon() {
		return new ItemStack(Waystone.INSTANCE);
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return RecipeRegistry.CycleWaystoneModelSerializer.get();
	}
}
