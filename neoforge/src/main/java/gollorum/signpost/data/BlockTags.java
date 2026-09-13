package gollorum.signpost.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.WaystoneBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class BlockTags extends BlockTagsProvider {

    public static final TagKey<Block> WaystoneTag = net.minecraft.tags.BlockTags.create(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "waystone"));
    public static final TagKey<Block> SignpostTag = net.minecraft.tags.BlockTags.create(Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "signpost"));

    public BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Signpost.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookupProvider) {
        this.tag(WaystoneTag)
            .add(ModelWaystone.variants.stream().map(v -> key(v.getBlock())).toArray(ResourceKey[]::new))
            .add(key(WaystoneBlock.getInstance()));

        this.tag(SignpostTag)
            .add(PostBlock.all().map(BlockTags::key).toArray(ResourceKey[]::new));
        this.tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
            .add(PostBlock.all()
                .filter(v -> v.materialType.tool == PostBlock.RequiredTool.Axe)
                .map(BlockTags::key).toArray(ResourceKey[]::new));
        this.tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
            .add(PostBlock.all()
                .filter(v -> v.materialType.tool == PostBlock.RequiredTool.Pickaxe)
                .map(BlockTags::key).toArray(ResourceKey[]::new))
            .add(ModelWaystone.variants.stream().map(v -> key(v.getBlock())).toArray(ResourceKey[]::new))
            .add(key(WaystoneBlock.getInstance()));
    }

    private static ResourceKey<Block> key(Block block) {
        return block.builtInRegistryHolder().key();
    }
}
