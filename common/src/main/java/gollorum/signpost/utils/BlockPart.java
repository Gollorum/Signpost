package gollorum.signpost.utils;

import com.mojang.serialization.MapCodec;
import gollorum.signpost.interactions.Interactable;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.security.WithOwner;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collection;

public interface BlockPart<T extends BlockPart<T>> extends Interactable {

    MapCodec<BlockPart> CODEC = BlockPartMetadata.CODEC.<BlockPart>dispatchMap(
        BlockPart::getMeta,
        BlockPartMetadata::codec);

    StreamCodec<RegistryFriendlyByteBuf, BlockPart> STREAM_CODEC = BlockPartMetadata.STREAM_CODEC.<BlockPart>dispatch(
        BlockPart::getMeta,
        BlockPartMetadata::streamCodec
    );

    BlockPartMetadata<T> getMeta();

    boolean hasThePermissionToEdit(WithOwner tile, @Nullable Player player);

    Collection<ItemStack> getDrops();

    default void attachTo(PostTile tile) {}
    default void removeFrom(PostTile tile) {}

    Collection<Texture> getAllTextures();

}