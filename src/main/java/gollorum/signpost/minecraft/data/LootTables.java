package gollorum.signpost.minecraft.data;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.LootTableProvider;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.functions.CopyNbt;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LootTables extends LootTableProvider {

    public LootTables(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables() {
        return ImmutableList.of(Pair.of(() -> this::generateBlockLootTables, LootParameterSets.BLOCK));
    }

    @Override
    protected void validate(Map<ResourceLocation, LootTable> map, ValidationTracker validationtracker) { }

    private void generateBlockLootTables(BiConsumer<ResourceLocation, LootTable.Builder> builder) {
        for(PostBlock.Variant variant : PostBlock.AllVariants)
            builder.accept(
                new ResourceLocation(Signpost.MOD_ID, "blocks/" + variant.block.getRegistryName().getPath()),
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setRolls(ConstantRange.exactly(1))
                        .add(ItemLootEntry.lootTableItem(variant.block)
                            .apply(CopyNbt.copyData(CopyNbt.Source.BLOCK_ENTITY).copy("Parts", "Parts"))
                            .when(BlockLootTables.HAS_SILK_TOUCH)
                            .otherwise(ItemLootEntry.lootTableItem(variant.block))
                        )
                    )
            );
        builder.accept(
            new ResourceLocation(Signpost.MOD_ID, "blocks/" + WaystoneBlock.INSTANCE.getRegistryName().getPath()),
            BlockLootTables.createSingleItemTable(WaystoneBlock.INSTANCE, ConstantRange.exactly(1)));
        for(ModelWaystone.Variant variant : ModelWaystone.variants)
            builder.accept(
                new ResourceLocation(Signpost.MOD_ID, "blocks/" + variant.block.getRegistryName().getPath()),
                BlockLootTables.createSingleItemTable(variant.block, ConstantRange.exactly(1)));
    }

}
