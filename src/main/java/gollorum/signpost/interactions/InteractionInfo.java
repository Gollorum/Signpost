package gollorum.signpost.interactions;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;

import java.util.function.Consumer;

public class InteractionInfo {

    public static enum Type {
        LeftClick, RightClick
    }

    public final Type type;
    public final PlayerEntity player;
    public final TileEntity tile;
    public final Consumer<CompoundNBT> mutationDistributor;

    public InteractionInfo(Type type, PlayerEntity player, TileEntity tile, Consumer<CompoundNBT> mutationDistributor) {
        this.type = type;
        this.player = player;
        this.tile = tile;
        this.mutationDistributor = mutationDistributor;
    }
}
