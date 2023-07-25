package gollorum.signpost.minecraft.data;

import com.google.common.collect.ImmutableList;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.storage.loot.PermissionCheck;
import gollorum.signpost.minecraft.storage.loot.RegisteredWaystoneLootNbtProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class LootTables extends LootTableProvider {

    public LootTables(PackOutput packOutput) {
        super(packOutput, Set.of(), VanillaLootTableProvider.create(packOutput).getTables());
    }

    @Override
    public List<SubProviderEntry> getTables() {
        return ImmutableList.of(new SubProviderEntry(() -> this::generateBlockLootTables, LootContextParamSets.BLOCK));
    }

    @Override
    protected void validate(Map<ResourceLocation, LootTable> map, ValidationContext validationtracker) { }

    private void generateBlockLootTables(BiConsumer<ResourceLocation, LootTable.Builder> builder) {
        for(PostBlock.Variant variant : PostBlock.AllVariants)
            builder.accept(
                new ResourceLocation(Signpost.MOD_ID, "blocks/" + ForgeRegistries.BLOCKS.getKey(variant.getBlock()).getPath()),
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(variant.getBlock())
                            .apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY).copy("Parts", "Parts"))
                            .when(BlockLootSubProvider.HAS_SILK_TOUCH)
                            .otherwise(LootItem.lootTableItem(variant.getBlock()))
                        )
                    )
            );
        builder.accept(
            new ResourceLocation(Signpost.MOD_ID, "blocks/" + ForgeRegistries.BLOCKS.getKey(WaystoneBlock.getInstance()).getPath()),
            mkWaystoneLootTable(WaystoneBlock.getInstance()));
        for(ModelWaystone.Variant variant : ModelWaystone.variants)
            builder.accept(
                new ResourceLocation(Signpost.MOD_ID, "blocks/" + ForgeRegistries.BLOCKS.getKey(variant.getBlock()).getPath()),
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
                    ).when(BlockLootSubProvider.HAS_SILK_TOUCH)
                    .when(new PermissionCheck.Builder(PermissionCheck.Type.CanPickWaystone))
                    .otherwise(LootItem.lootTableItem(block))));
    }

}
