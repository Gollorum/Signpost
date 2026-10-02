package gollorum.signpost.minecraft.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.utils.BlockPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.SingleEntryContainerBase;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;
import java.util.function.Consumer;

public class PostBlockPartDropLoot extends SingleEntryContainerBase {

    public static final MapCodec<PostBlockPartDropLoot> MAP_CODEC = RecordCodecBuilder.mapCodec(inst ->
        uniformFields(inst)
        .apply(inst, PostBlockPartDropLoot::new)
    );

    public static UniformContainerBase.Builder<?> createBuilder() {
        return simpleBuilder(PostBlockPartDropLoot::new);
    }

    protected PostBlockPartDropLoot(int weight, int quality, Optional<Holder<LootItemCondition>> condition, Optional<Holder<LootItemFunction>> modifier) {
        super(weight, quality, condition, modifier);
    }


    @Override
    protected void createItemStack(Consumer<ItemStack> consumer, LootContext lootContext) {
        if (lootContext.getOptional(LootContextParams.BLOCK_ENTITY) instanceof PostTile postTile)
            for (var part : postTile.getParts()) {
                for (var item : ((BlockPart<?>)part.blockPart()).getDrops()) {
                    consumer.accept(item);
                }
            }
        else if (lootContext.getOptional(LootContextParams.BLOCK_ENTITY) instanceof WaystoneTile) {
            consumer.accept(new ItemStack(Items.GLOW_ITEM_FRAME, 13));
        }
    }

    @Override
    public MapCodec<? extends SingleEntryContainerBase> codec() {
        return LootEntries.POST_BLOCK_LOOT;
    }
}
