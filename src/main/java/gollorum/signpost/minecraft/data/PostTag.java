package gollorum.signpost.minecraft.data;

import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.block.Block;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;

public class PostTag extends ItemTagsProvider {

    public static final String Id = "signpost";

    public static final Tag<Item> Tag = ItemTags.bind(Id);

    public PostTag(DataGenerator dataGenerator) {
        super(dataGenerator);
    }

    @Override
    protected void addTags() {
        this.tag(Tag)
            .add(PostBlock.AllVariants.stream().map(i -> i.block.asItem()).toArray(Item[]::new));
    }

    public static class Blocks extends BlockTagsProvider {

        public static final Tag<Block> Tag = BlockTags.bind(Id);

        public Blocks(DataGenerator generatorIn) {
            super(generatorIn);
        }

        @Override
        protected void addTags() {
            this.tag(Tag)
                .add(PostBlock.AllVariants.stream().map(i -> i.block).toArray(Block[]::new));
        }

    }

}