package gollorum.signpost.minecraft.items;

import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

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
        var tag = stack.getTags().filter(t -> t.location().equals(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "Handle"))).findAny();
        tag.ifPresent(t -> {
            tooltipComponents.add(Component.translatable(LangKeys.waystoneHasId));
            if(flag.isAdvanced()) tooltipComponents.add(Component.translatable(LangKeys.waystoneId,
                WaystoneHandle.Vanilla.CompoundSerializer.decode(t.getCompound("Handle"), ).id.toString()));
        });
    }
}
