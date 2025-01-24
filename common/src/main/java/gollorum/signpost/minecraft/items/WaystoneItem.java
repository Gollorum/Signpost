package gollorum.signpost.minecraft.items;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class WaystoneItem extends BlockItem {

    public WaystoneItem(WaystoneBlock waystone, Properties properties) {
        super(waystone, properties);
    }

    public WaystoneItem(ModelWaystone waystone, Properties properties) {
        super(waystone, properties);
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
