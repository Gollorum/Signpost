package gollorum.signpost.compat;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Teleport;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.PlayerUtils;
import gollorum.signpost.minecraft.utils.TextComponents;
import gollorum.signpost.networking.PacketHandler;
import hunternif.mc.impl.atlas.AntiqueAtlasMod;
import hunternif.mc.impl.atlas.marker.GlobalMarkersData;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;

public class RequestTeleportByMarkerEvent implements PacketHandler.Event<RequestTeleportByMarkerEvent.Packet> {

    public static final class Packet {
        public final int markerId;
        public Packet(int markerId) { this.markerId = markerId; }
    }

    @Override
    public Class<Packet> getMessageClass() { return Packet.class; }

    @Override
    public void encode(Packet message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.markerId);
    }

    @Override
    public Packet decode(FriendlyByteBuf buffer) {
        return new Packet(buffer.readInt());
    }

    @Override
    public void handle(Packet message, NetworkEvent.Context context) {
        assert context.getDirection().getReceptionSide().isServer();
        context.enqueueWork(() -> {
            GlobalMarkersData data = AntiqueAtlasMod.globalMarkersData.getData();
            data.getVisitedDimensions().stream()
                .flatMap(level -> data.getMarkersInWorld(level).stream())
                .filter(marker -> marker.getId() == message.markerId)
                .findFirst()
                .flatMap(marker -> WaystoneLibrary.getInstance().getHandleByName(marker.getLabel().getString()))
                .ifPresent(handle -> {
                    ServerPlayer player = context.getSender();
                    if(player == null) return;
                    potentiallyDiscover(handle, player);
                    potentiallyTeleportTo(handle, player);
                });
        });
    }

    private void potentiallyTeleportTo(WaystoneHandle.Vanilla handle, ServerPlayer player) {
        if(Config.Server.compat.atlas.enableTeleport.get()
            && (
                !Config.Server.compat.atlas.teleportRequiresSignpost.get()
                || PlayerUtils.findBlockLookedAtBy(player).filter(b -> b instanceof PostBlock).isPresent()
            )
        ) PacketHandler.send(
            PacketDistributor.PLAYER.with(() -> player),
            new Teleport.RequestGui.Package(
                Teleport.RequestGui.Package.Info.from(player, handle),
                Optional.empty()
            )
        );
        else player.displayClientMessage(new TranslatableComponent(LangKeys.noTeleportAntiqueAtlasMod), true);
    }

    private void potentiallyDiscover(WaystoneHandle.Vanilla handle, ServerPlayer player) {
        if(Config.Server.compat.atlas.enableDiscovery.get()
            && WaystoneLibrary.getInstance().addDiscovered(PlayerHandle.from(player), handle)
        ) WaystoneLibrary.getInstance().getData(handle).ifPresent(waystoneData ->
            player.sendMessage(new TranslatableComponent(LangKeys.discovered, TextComponents.waystone(player, waystoneData.name)), Util.NIL_UUID)
        );
    }

}