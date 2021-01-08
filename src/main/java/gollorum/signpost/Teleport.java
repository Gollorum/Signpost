package gollorum.signpost;

import gollorum.signpost.minecraft.Config;
import gollorum.signpost.minecraft.gui.ConfirmTeleportGui;
import gollorum.signpost.minecraft.gui.LangKeys;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.TileEntityUtils;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class Teleport {

    public static void toWaystone(WaystoneHandle waystone, PlayerEntity player){
        assert Signpost.getServerType().isServer;
        WaystoneLocationData waystoneData = WaystoneLibrary.getInstance().getLocationData(waystone);
        waystoneData.block.world.mapLeft(Optional::of)
            .leftOr(i -> TileEntityUtils.findWorld(i, false))
        .ifPresent(unspecificWorld -> {
            if(!(unspecificWorld instanceof ServerWorld)) return;
            ServerWorld world = (ServerWorld) unspecificWorld;
            Vector3 location = waystoneData.spawn;
            Vector3 diff = Vector3.fromBlockPos(waystoneData.block.blockPos).add(new Vector3(0.5f, 0.5f, 0.5f))
                .subtract(location.withY(y -> y + player.getEyeHeight()));
            Angle yaw = Angle.between(
                0, 1,
                diff.x, diff.z
            );
            Angle pitch = Angle.fromRadians((float) (Math.PI / 2 + Math.atan(Math.sqrt(diff.x * diff.x + diff.z * diff.z) / diff.y)));
            if(!player.world.getDimensionType().equals(world.getDimensionType()))
                player.changeDimension(world, new ITeleporter() {});
            player.rotationYaw = yaw.degrees();
            player.rotationPitch = pitch.degrees();
            player.setPositionAndUpdate(location.x, location.y, location.z);
        });
    }

    public static void requestOnClient(String waystoneName) {
        ConfirmTeleportGui.display(waystoneName);
    }

    public static final class Request implements PacketHandler.Event<Request.Package> {

        @Override
        public Class<Package> getMessageClass() {
            return Package.class;
        }

        @Override
        public void encode(Package message, PacketBuffer buffer) {
            buffer.writeString(message.waystoneName);
        }

        @Override
        public Package decode(PacketBuffer buffer) {
            return new Package(buffer.readString(32767));
        }

        @Override
        public void handle(
            Package message, Supplier<NetworkEvent.Context> contextGetter
        ) {
            NetworkEvent.Context context = contextGetter.get();
            context.enqueueWork(() -> {
                if(context.getDirection().getReceptionSide().isServer()) {
                    Optional<WaystoneHandle> waystone = WaystoneLibrary.getInstance().getHandleByName(message.waystoneName);
                    if(waystone.isPresent()){
                        Teleport.toWaystone(waystone.get(), context.getSender());
                    } else context.getSender().sendMessage(
                        new TranslationTextComponent(LangKeys.waystoneNotFound, message.waystoneName),
                        Util.DUMMY_UUID
                    );
                } else {
                    if(Config.Client.enableConfirmationScreen.get()) requestOnClient(message.waystoneName);
                    else PacketHandler.sendToServer(new Teleport.Request.Package(message.waystoneName));
                }
            });
        }

        public static final class Package {
            public final String waystoneName;
            public Package(String waystoneName) {
                this.waystoneName = waystoneName;
            }
        }

    }

}
