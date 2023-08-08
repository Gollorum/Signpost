package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class BlockTags extends BlockTagsProvider {

    public static final TagKey<Block> WaystoneTag = net.minecraft.tags.BlockTags.create(new ResourceLocation(Signpost.MOD_ID, "waystone"));
    public static final TagKey<Block> SignpostTag = net.minecraft.tags.BlockTags.create(new ResourceLocation(Signpost.MOD_ID, "signpost"));

    public BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper fileHelper) {
        super(output, lookupProvider, Signpost.MOD_ID, fileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookupProvider) {
        this.tag(WaystoneTag)
            .add(ModelWaystone.variants.stream().map(ModelWaystone.Variant::getBlock).toArray(Block[]::new))
            .add(WaystoneBlock.getInstance());

        this.tag(SignpostTag)
            .add(PostBlock.AllVariants.stream().map(PostBlock.Variant::getBlock).toArray(Block[]::new));
        this.tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE).add(PostBlock.AllVariants.stream()
            .filter(v -> v.tool == PostBlock.Variant.RequiredTool.Axe)
            .map(PostBlock.Variant::getBlock).toArray(Block[]::new));
        this.tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(PostBlock.AllVariants.stream()
                .filter(v -> v.tool == PostBlock.Variant.RequiredTool.Pickaxe)
                .map(PostBlock.Variant::getBlock).toArray(Block[]::new))
            .add(ModelWaystone.variants.stream().map(ModelWaystone.Variant::getBlock).toArray(Block[]::new))
            .add(WaystoneBlock.getInstance());
    }

}