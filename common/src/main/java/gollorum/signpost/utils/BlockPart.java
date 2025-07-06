package gollorum.signpost.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.security.WithOwner;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Collection;

public interface BlockPart<T extends BlockPart<T>> extends Interactable {

    record SerializedRepresentation<T extends BlockPart<T>>(BlockPartMetadata<T> metadata, CompoundTag tag) {
        public static <T extends BlockPart<T>> SerializedRepresentation<T> from(BlockPart<T> part, HolderLookup.Provider registryAccess) {
            return new SerializedRepresentation<>(part.getMeta(), part.write(registryAccess));
        }

        public T deserialize(HolderLookup.Provider registryAccess) {
            return metadata.read.apply(tag, registryAccess);
        }
    }

    Codec<SerializedRepresentation> CODEC = Codec.pair(
        Codec.string(3, 20).xmap(
            PostTile.partsMetadata::get,
            meta -> meta.identifier()
        ),
        Codec.withAlternative(CompoundTag.CODEC, TagParser.LENIENT_CODEC)
    ).xmap(
        pair -> new SerializedRepresentation(pair.getFirst(), pair.getSecond()),
        rep -> Pair.of(rep.metadata, rep.tag)
    );

    BlockPartMetadata<T> getMeta();

    default CompoundTag write(HolderLookup.Provider provider){
        CompoundTag compound = new CompoundTag();
        writeTo(compound, provider);
        return compound;
    }
    void writeTo(CompoundTag compound, HolderLookup.Provider provider);

    void readMutationUpdate(CompoundTag compound, BlockEntity tile, @Nullable Player editingPlayer, HolderLookup.Provider holderLookupProvider);
    boolean hasThePermissionToEdit(WithOwner tile, @Nullable Player player);

    Collection<ItemStack> getDrops(PostTile tile);

    default void attachTo(PostTile tile) {}
    default void removeFrom(PostTile tile) {}

    Collection<Texture> getAllTextures();

}