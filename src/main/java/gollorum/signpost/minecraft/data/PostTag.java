package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.Post;
import net.minecraft.block.Block;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Arrays;

public class PostTag extends ItemTagsProvider {

    public static final String Id = "signpost";

    public static final ITag.INamedTag<Item> Tag = ItemTags.makeWrapperTag(Id);

    public PostTag(DataGenerator dataGenerator, Blocks blockTagProvider, ExistingFileHelper fileHelper) {
        super(dataGenerator, blockTagProvider, Signpost.MOD_ID, fileHelper);
    }

    @Override
    protected void registerTags() {
        this.getOrCreateBuilder(Tag)
            .add(Post.AllVariants.stream().map(i -> i.block.asItem()).toArray(Item[]::new));
    }

    public static class Blocks extends BlockTagsProvider {

        public static final ITag.INamedTag<Block> Tag = BlockTags.makeWrapperTag(Id);

        public Blocks(DataGenerator generatorIn, ExistingFileHelper fileHelper) {
            super(generatorIn, Signpost.MOD_ID, fileHelper);
        }

        @Override
        protected void registerTags() {
            this.getOrCreateBuilder(Tag)
                .add(Post.AllVariants.stream().map(i -> i.block).toArray(Block[]::new));
        }

    }

}