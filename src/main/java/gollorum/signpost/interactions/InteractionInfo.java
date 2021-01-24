package gollorum.signpost.interactions;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;

import java.util.function.Consumer;

public class InteractionInfo {

    public static enum Type {
        LeftClick, RightClick
    }

    public final Type type;
    public final PlayerEntity player;
    public final Hand hand;
    public final PostTile tile;
    public final PostTile.TraceResult traceResult;
    public final Consumer<CompoundNBT> mutationDistributor;
    public final boolean isRemote;

    public InteractionInfo(Type type, PlayerEntity player, Hand hand, PostTile tile, PostTile.TraceResult traceResult, Consumer<CompoundNBT> mutationDistributor, boolean isRemote) {
        this.type = type;
        this.player = player;
        this.hand = hand;
        this.tile = tile;
        this.traceResult = traceResult;
        this.mutationDistributor = mutationDistributor;
        this.isRemote = isRemote;
    }

    public PostTile.TilePartInfo getTilePartInfo() { return new PostTile.TilePartInfo(tile, traceResult.id); }
}
