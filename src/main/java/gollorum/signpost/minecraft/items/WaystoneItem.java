package gollorum.signpost.minecraft.items;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WaystoneItem extends BlockItem {

    public WaystoneItem(WaystoneBlock waystone, Properties properties) {
        super(waystone, properties);
    }

    public WaystoneItem(ModelWaystone waystone, Properties properties) {
        super(waystone, properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> hoverText, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, hoverText, flag);

        if(stack.hasTag() && stack.getTag().contains("Handle")) {
            hoverText.add(new TranslatableComponent(LangKeys.waystoneHasId));
            if(flag.isAdvanced()) hoverText.add(new TranslatableComponent(LangKeys.waystoneId,
                WaystoneHandle.Vanilla.Serializer.read(stack.getTag().getCompound("Handle")).id.toString()));
        }
    }
}
