package gollorum.signpost.interactions;

import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.function.Consumer;

public class InteractionInfo {

    public static enum Type {
        LeftClick, RightClick
    }

    public final Type type;
    public final PlayerEntity player;
    public final PostTile tile;
    public final Vector3 localHitPos;
    public final Consumer<CompoundNBT> mutationDistributor;
    public final boolean isRemote;

    public InteractionInfo(Type type, PlayerEntity player, PostTile tile, Vector3 localHitPos, Consumer<CompoundNBT> mutationDistributor, boolean isRemote) {
        this.type = type;
        this.player = player;
        this.tile = tile;
        this.localHitPos = localHitPos;
        this.mutationDistributor = mutationDistributor;
        this.isRemote = isRemote;
    }
}
