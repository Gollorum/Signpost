package gollorum.signpost.minecraft.items;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.data.WaystoneHandleData;
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
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, flag);
        var data = stack.get(WaystoneHandleData.TYPE);
        if(data != null) {
            tooltipComponents.accept(Component.translatable(LangKeys.waystoneHasId));
            if(flag.isAdvanced()) tooltipComponents.accept(Component.translatable(LangKeys.waystoneId, data.handle().id().toString()));
        }
    }
}
