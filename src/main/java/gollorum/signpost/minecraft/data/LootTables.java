package gollorum.signpost.minecraft.data;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

;

public class LootTables extends LootTableProvider {

    public LootTables(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables() {
        return ImmutableList.of(Pair.of(() -> this::generateBlockLootTables, LootContextParamSets.BLOCK));
    }

    @Override
    protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationtracker) { }

    private void generateBlockLootTables(BiConsumer<ResourceLocation, LootTable.Builder> builder) {
        for(PostBlock.Variant variant : PostBlock.AllVariants)
            builder.accept(
                new ResourceLocation(Signpost.MOD_ID, "blocks/" + variant.block.getRegistryName().getPath()),
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(variant.block)
                            .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY).copy("Parts", "Parts"))
                            .when(BlockLoot.HAS_SILK_TOUCH)
                            .otherwise(LootItem.lootTableItem(variant.block))
                        )
                    )
            );
        builder.accept(
            new ResourceLocation(Signpost.MOD_ID, "blocks/" + WaystoneBlock.INSTANCE.getRegistryName().getPath()),
            BlockLoot.createSingleItemTable(WaystoneBlock.INSTANCE, ConstantValue.exactly(1)));
        for(ModelWaystone.Variant variant : ModelWaystone.variants)
            builder.accept(
                new ResourceLocation(Signpost.MOD_ID, "blocks/" + variant.block.getRegistryName().getPath()),
                BlockLoot.createSingleItemTable(variant.block, ConstantValue.exactly(1)));
    }

}