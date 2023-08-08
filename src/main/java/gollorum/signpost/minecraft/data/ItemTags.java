package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ItemTags extends ItemTagsProvider {

    public static final TagKey<Item> WaystoneTag = net.minecraft.tags.ItemTags.create(new ResourceLocation(Signpost.MOD_ID, "waystone"));
    public static final TagKey<Item> SignpostTag = net.minecraft.tags.ItemTags.create(new ResourceLocation(Signpost.MOD_ID, "signpost"));

    public ItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, BlockTags blockTagProvider, ExistingFileHelper fileHelper) {
        super(output, lookupProvider, blockTagProvider.contentsGetter(), Signpost.MOD_ID, fileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookupProvider) {
        this.tag(WaystoneTag)
            .add(ModelWaystone.variants.stream().map(i -> i.getBlock().asItem()).toArray(Item[]::new))
            .add(WaystoneBlock.getInstance().asItem());

        this.tag(SignpostTag)
            .add(PostBlock.AllVariants.stream().map(i -> i.getBlock().asItem()).toArray(Item[]::new));
    }
}
