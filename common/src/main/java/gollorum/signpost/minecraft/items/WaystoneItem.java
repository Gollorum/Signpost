package gollorum.signpost.minecraft.items;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class WaystoneItem extends BlockItem {

    public WaystoneItem(WaystoneBlock waystone, Properties properties) {
        super(waystone, properties
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, WaystoneBlock.REGISTRY_NAME))));
    }

    public WaystoneItem(ModelWaystone waystone, Properties properties) {
        super(waystone, properties
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, waystone.variant.registryName))));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipComponents, flag);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if(data.contains("Handle")) {
            tooltipComponents.add(Component.translatable(LangKeys.waystoneHasId));
            if(flag.isAdvanced()) tooltipComponents.add(Component.translatable(LangKeys.waystoneId,
                WaystoneHandle.Vanilla.CompoundSerializer.decode(data.copyTag().getCompound("Handle"), context.registries()).id.toString()));
        }
    }
}
