package gollorum.signpost.data;

import com.google.common.collect.ImmutableList;
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
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class LootTables extends LootTableProvider {

    public LootTables(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registryAccess) {
        super(packOutput, Set.of(), VanillaLootTableProvider.create(packOutput, registryAccess).getTables(), registryAccess);
    }

    @Override
    public List<SubProviderEntry> getTables() {
        return ImmutableList.of(new SubProviderEntry(
            registryAccess -> builder -> generateBlockLootTables(registryAccess, builder),
            LootContextParamSets.BLOCK));
    }


    private void generateBlockLootTables(HolderLookup.Provider registryAccess, BiConsumer<ResourceKey<LootTable>, LootTable.Builder> builder) {
        for(PostBlock.Variant variant : PostBlock.AllVariants)
            builder.accept(
                ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + BuiltInRegistries.BLOCK.getKey(variant.getBlock()).getPath())),
                mkPostLootTable(registryAccess, variant)
            );

        builder.accept(
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + BuiltInRegistries.BLOCK.getKey(WaystoneBlock.getInstance()).getPath())),
            mkWaystoneLootTable(registryAccess, WaystoneBlock.getInstance()));
        for(ModelWaystone.Variant variant : ModelWaystone.variants)
            builder.accept(
                ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + BuiltInRegistries.BLOCK.getKey(variant.getBlock()).getPath())),
                mkWaystoneLootTable(registryAccess, variant.getBlock()));
    }

    private LootTable.Builder mkWaystoneLootTable(HolderLookup.Provider registryAccess, Block block) {
        var includeDataCondition = hasSilkTouch(registryAccess).and(new PermissionCheck.Builder(PermissionCheck.Type.CanPickWaystone));
        return LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                    .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(new ContextKey<WaystoneTile>(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, WaystoneTile.REGISTRY_NAME)))
                        .include(WaystoneHandleData.TYPE)
                        .include(DataComponents.CUSTOM_NAME)
                    ).when(includeDataCondition)
                    .otherwise(LootItem.lootTableItem(block))));
    }

    private LootTable.Builder mkPostLootTable(HolderLookup.Provider registryAccess, PostBlock.Variant variant) {
        return LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(variant.getBlock())
                    .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(new ContextKey<PostTile>(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, PostTile.REGISTRY_NAME)))
                        .include(WaystoneHandleData.TYPE)
                        .include(DataComponents.CUSTOM_NAME)
                        .include(PostData.TYPE))
                    .when(hasSilkTouch(registryAccess))
                    .otherwise(LootItem.lootTableItem(variant.getBlock()))))
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(PostBlockPartDropLoot.createBuilder()
                    .when(hasSilkTouch(registryAccess).invert())));
    }

    private LootItemCondition.Builder hasSilkTouch(HolderLookup.Provider registryAccess) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        return MatchTool.toolMatches(
            ItemPredicate.Builder.item()
                .withComponents(
                    DataComponentMatchers.Builder.components()
                        .partial(
                            DataComponentPredicates.ENCHANTMENTS,
                            EnchantmentsPredicate.enchantments(
                                List.of(
                                    new EnchantmentPredicate(
                                        registrylookup.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)
                                    )
                                )
                            )
                        )
                        .build()
                )
        );
    }


}
