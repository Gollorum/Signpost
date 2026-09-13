package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class ItemTags extends TagsProvider<Item> {

    public static final TagKey<Item> WaystoneTag = net.minecraft.tags.ItemTags.create(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "waystone"));
    public static final TagKey<Item> SignpostTag = net.minecraft.tags.ItemTags.create(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "signpost"));

    public ItemTags(BlockTags blockTags, PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider);
    }

    @Override
    @SuppressWarnings("unchecked") // ResourceKey[] into the add(ResourceKey<Item>...) varargs
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(WaystoneTag)
            .add(ModelWaystone.variants.stream().map(i -> key(i.getBlock().asItem())).toArray(ResourceKey[]::new))
            .add(key(WaystoneBlock.getInstance().asItem()));

        this.tag(SignpostTag)
            .add(PostBlock.all().map(b -> key(b.asItem())).toArray(ResourceKey[]::new));
    }

    private static ResourceKey<Item> key(Item item) {
        return item.builtInRegistryHolder().key();
    }
}
