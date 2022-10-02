package gollorum.signpost.minecraft.data;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.storage.loot.PermissionCheck;
import gollorum.signpost.minecraft.storage.loot.RegisteredWaystoneLootNbtProvider;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
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
                new ResourceLocation(Signpost.MOD_ID, "blocks/" + Registry.BLOCK.getKey(variant.getBlock()).getPath()),
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(variant.getBlock())
                            .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY).copy("Parts", "Parts"))
                            .when(BlockLoot.HAS_SILK_TOUCH)
                            .otherwise(LootItem.lootTableItem(variant.getBlock()))
                        )
                    )
            );
        builder.accept(
            new ResourceLocation(Signpost.MOD_ID, "blocks/" + Registry.BLOCK.getKey(WaystoneBlock.getInstance()).getPath()),
            mkWaystoneLootTable(WaystoneBlock.getInstance()));
        for(ModelWaystone.Variant variant : ModelWaystone.variants)
            builder.accept(
                new ResourceLocation(Signpost.MOD_ID, "blocks/" + Registry.BLOCK.getKey(variant.getBlock()).getPath()),
                mkWaystoneLootTable(variant.getBlock()));
    }

    private LootTable.Builder mkWaystoneLootTable(Block block) {
        return LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                    .apply(CopyNbtFunction.copyData(new RegisteredWaystoneLootNbtProvider())
                        .copy("Handle", "Handle")
                        .copy("display", "display", CopyNbtFunction.MergeStrategy.MERGE)
                    ).when(BlockLoot.HAS_SILK_TOUCH)
                    .when(new PermissionCheck.Builder(PermissionCheck.Type.CanPickWaystone))
                    .otherwise(LootItem.lootTableItem(block))));
    }

}
