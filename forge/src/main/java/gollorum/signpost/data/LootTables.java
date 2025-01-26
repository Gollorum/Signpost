package gollorum.signpost.data;

import com.google.common.collect.ImmutableList;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import gollorum.signpost.minecraft.storage.loot.PermissionCheck;
import gollorum.signpost.minecraft.storage.loot.RegisteredWaystoneLootNbtProvider;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyCustomDataFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.registries.ForgeRegistries;

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

    @Override
    protected void validate(Registry<LootTable> map, ValidationContext validationcontext, ProblemReporter report) { }

    private void generateBlockLootTables(HolderLookup.Provider registryAccess, BiConsumer<ResourceKey<LootTable>, LootTable.Builder> builder) {
        for(PostBlock.Variant variant : PostBlock.AllVariants)
            builder.accept(
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + ForgeRegistries.BLOCKS.getKey(variant.getBlock()).getPath())),
                LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(variant.getBlock())
                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(DataComponents.CUSTOM_DATA))
                            .when(hasSilkTouch(registryAccess))
                            .otherwise(LootItem.lootTableItem(variant.getBlock()))
                        )
                    )
            );
        builder.accept(
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + ForgeRegistries.BLOCKS.getKey(WaystoneBlock.getInstance()).getPath())),
            mkWaystoneLootTable(registryAccess, WaystoneBlock.getInstance()));
        for(ModelWaystone.Variant variant : ModelWaystone.variants)
            builder.accept(
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "blocks/" + ForgeRegistries.BLOCKS.getKey(variant.getBlock()).getPath())),
                mkWaystoneLootTable(registryAccess, variant.getBlock()));
    }

    private LootTable.Builder mkWaystoneLootTable(HolderLookup.Provider registryAccess, Block block) {
        return LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                    .apply(CopyCustomDataFunction.copyData(new RegisteredWaystoneLootNbtProvider())
                        .copy("Handle", "Handle")
                        .copy("display", "display", CopyCustomDataFunction.MergeStrategy.MERGE)
                    ).when(hasSilkTouch(registryAccess))
                    .when(new PermissionCheck.Builder(PermissionCheck.Type.CanPickWaystone))
                    .otherwise(LootItem.lootTableItem(block))));
    }

    private LootItemCondition.Builder hasSilkTouch(HolderLookup.Provider registryAccess) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        return MatchTool.toolMatches(ItemPredicate.Builder.item().withSubPredicate(ItemSubPredicates.ENCHANTMENTS, ItemEnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(registrylookup.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1))))));
    }


}
