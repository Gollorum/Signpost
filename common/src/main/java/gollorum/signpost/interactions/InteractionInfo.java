package gollorum.signpost.interactions;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public class InteractionInfo {

    public static enum Type {
        LeftClick, RightClick
    }

    public final Type type;
    public final Player player;
    public final InteractionHand hand;
    public final PostTile tile;
    public final PostTile.TraceResult traceResult;
    public final Runnable mutationDistributor;
    public final boolean isRemote;

    public InteractionInfo(Type type, Player player, InteractionHand hand, PostTile tile, PostTile.TraceResult traceResult, Runnable mutationDistributor, boolean isRemote) {
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
