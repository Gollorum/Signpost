package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.block.tiles.WaystoneTile;
import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.data.WaystoneHandleData;
import gollorum.signpost.minecraft.loot.PostBlockPartDropLoot;
import gollorum.signpost.minecraft.loot.PermissionCheck;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.List;
import java.util.Set;

// 26.3 made loot tables a reloadable datapack registry: the provider is now a bootstrap for Registries.LOOT_TABLE,
// and each sub provider writes through the LootTableSubProvider.Context it is constructed with.
public class LootTables implements LootTableSubProvider {

    public static LootTableProvider create() {
        return new LootTableProvider(Set.of(), List.of(new LootTableProvider.SubProviderEntry(LootTables::new, LootContextParamSets.BLOCK)));
    }

    private final LootTableSubProvider.Context builder;
    private final HolderGetter<Enchantment> enchantments;

    private LootTables(LootTableSubProvider.Context builder) {
        this.builder = builder;
        this.enchantments = builder.lookup(Registries.ENCHANTMENT);
    }

    @Override
    public void run() {
        PostBlock.all().forEach(block ->
            builder.accept(
                ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + BuiltInRegistries.BLOCK.getKey(block).getPath())),
                mkPostLootTable(block)
            ));

        builder.accept(
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + BuiltInRegistries.BLOCK.getKey(WaystoneBlock.getInstance()).getPath())),
            mkWaystoneLootTable(WaystoneBlock.getInstance()));
        for(ModelWaystone.Variant variant : ModelWaystone.variants)
            builder.accept(
                ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + BuiltInRegistries.BLOCK.getKey(variant.getBlock()).getPath())),
                mkWaystoneLootTable(variant.getBlock()));
    }

    private LootTable.Builder mkWaystoneLootTable(Block block) {
        var includeDataCondition = hasSilkTouch().and(new PermissionCheck.Builder(PermissionCheck.Type.CanPickWaystone));
        return LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(block)
                    .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(WaystoneHandleData.TYPE)
                        .include(DataComponents.CUSTOM_NAME)
                    ).when(includeDataCondition)
                    .otherwise(LootItem.lootTableItem(block))));
    }

    private LootTable.Builder mkPostLootTable(PostBlock block) {
        var drop = block.materialType.getBlock();
        return LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(drop)
                    .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(WaystoneHandleData.TYPE)
                        .include(DataComponents.CUSTOM_NAME)
                        .include(PostData.TYPE))
                    .when(hasSilkTouch())
                    .otherwise(LootItem.lootTableItem(drop))))
            .withPool(LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(PostBlockPartDropLoot.createBuilder()
                    .when(hasSilkTouch().invert())));
    }

    private LootItemCondition.Builder hasSilkTouch() {
        return MatchTool.toolMatches(
            ItemPredicate.Builder.item()
                .withComponents(
                    DataComponentMatchers.Builder.components()
                        .partial(
                            DataComponentPredicates.ENCHANTMENTS,
                            EnchantmentsPredicate.enchantments(
                                List.of(
                                    new EnchantmentPredicate(
                                        enchantments.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)
                                    )
                                )
                            )
                        )
                        .build()
                )
        );
    }


}
