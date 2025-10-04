package gollorum.signpost.data;

import gollorum.signpost.registry.ItemRegistry;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

public class Items {

    public static void register(ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ItemRegistry.WRENCH.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ItemRegistry.BRUSH.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ItemRegistry.GENERATION_WAND.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
    }

}
