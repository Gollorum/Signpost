package gollorum.signpost.minecraft.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.data.WaystoneHandleData;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Optional;
import java.util.Set;

// TODO: Register or yeet
public final class RegisteredWaystoneLootDataFunction implements LootItemFunction {

    public static LootItemFunctionType<RegisteredWaystoneLootDataFunction> TYPE = new LootItemFunctionType<>(MapCodec.unit(RegisteredWaystoneLootDataFunction::new));

    @Override
    public ItemStack apply(ItemStack itemStack, LootContext context) {
        BlockEntity blockEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(blockEntity instanceof WaystoneTile waystoneTile) {
            Optional<WaystoneHandle.Vanilla> handle = waystoneTile.getHandle()
                .or(() -> WaystoneLibrary.getInstance().getHandleByLocation(WorldLocation.from(waystoneTile.getBlockPos(), waystoneTile.getLevel())));
            handle.ifPresent(h -> itemStack.set(WaystoneHandleData.TYPE, new WaystoneHandleData(h)));
            waystoneTile.getName()
                .or(() -> handle.flatMap(h -> WaystoneLibrary.getInstance().getData(h).map(WaystoneData::name)))
                .ifPresent(n -> itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(n)));
        }
        return itemStack;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    public LootItemFunctionType<? extends LootItemFunction> getType() {
        return TYPE;
    }

}
