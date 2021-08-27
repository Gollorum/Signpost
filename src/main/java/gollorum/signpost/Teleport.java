package gollorum.signpost;

import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.ConfirmTeleportGui;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.Inventory;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.relations.ExternalWaystone;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;

public class Teleport {

    public static void toWaystone(WaystoneHandle waystone, PlayerEntity player) {
        assert Signpost.getServerType().isServer;
        if(waystone instanceof WaystoneHandle.Vanilla) {
            WaystoneLocationData waystoneData = WaystoneLibrary.getInstance().getLocationData((WaystoneHandle.Vanilla) waystone);
            toWaystone(waystoneData, player);
        } else Signpost.LOGGER.error("Tried to teleport to non-vanilla waystone " + ((ExternalWaystone.Handle)waystone).modMark());
    }

    public static void toWaystone(WaystoneLocationData waystoneData, PlayerEntity player){
        assert Signpost.getServerType().isServer;
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

    public static void requestOnClient(
        Either<String, RequestGui.Package.Info> data,
        Optional<ConfirmTeleportGui.SignInfo> signInfo
    ) {
        ConfirmTeleportGui.display(data, signInfo);
    }

    public static ItemStack getCost(PlayerEntity player, Vector3 from, Vector3 to) {
        Item item = Registry.ITEM.getOrDefault(new ResourceLocation(Config.Server.teleport.costItem.get()));
        if(item.equals(Items.AIR) || player.isCreative() || player.isSpectator()) return ItemStack.EMPTY;
        int distancePerPayment = Config.Server.teleport.distancePerPayment.get();
        int distanceDependentCost = distancePerPayment < 0
            ? 0
            : (int)(from.distanceTo(to) / distancePerPayment);
        return new ItemStack(item, Config.Server.teleport.constantPayment.get() + distanceDependentCost);
    }

    public static final class Request implements PacketHandler.Event<Request.Package> {

        public static final class Package {
            public final String waystoneName;
            public Package(
                String waystoneName
            ) {
                this.waystoneName = waystoneName;
            }
        }

        @Override
        public Class<Package> getMessageClass() {
            return Package.class;
        }

        @Override
        public void encode(Package message, PacketBuffer buffer) {
            StringSerializer.instance.write(message.waystoneName, buffer);
        }

        @Override
        public Package decode(PacketBuffer buffer) {
            return new Package(StringSerializer.instance.read(buffer));
        }

        @Override
        public void handle(
            Package message, NetworkEvent.Context context
        ) {
            ServerPlayerEntity player = context.getSender();
            Optional<WaystoneHandle.Vanilla> waystone = WaystoneLibrary.getInstance().getHandleByName(message.waystoneName);
            if(waystone.isPresent()) {
                WaystoneHandle.Vanilla handle = waystone.get();
                WaystoneLocationData waystoneData = WaystoneLibrary.getInstance().getLocationData(handle);

                boolean isDiscovered = WaystoneLibrary.getInstance().isDiscovered(PlayerHandle.from(player), handle)
                    || !Config.Server.teleport.enforceDiscovery.get();
                int distance = (int) waystoneData.spawn.distanceTo(Vector3.fromVec3d(player.getPositionVec()));
                int maxDistance = Config.Server.teleport.maximumDistance.get();
                boolean isTooFarAway = maxDistance > 0 && distance > maxDistance;
                if(!isDiscovered) player.sendMessage(new TranslationTextComponent(LangKeys.notDiscovered, message.waystoneName), Util.DUMMY_UUID);
                if(isTooFarAway) player.sendMessage(new TranslationTextComponent(LangKeys.tooFarAway, Integer.toString(distance), Integer.toString(maxDistance)), Util.DUMMY_UUID);
                if(!isDiscovered || isTooFarAway) return;

                Inventory.tryPay(
                    player,
                    Teleport.getCost(player, Vector3.fromBlockPos(waystoneData.block.blockPos), waystoneData.spawn),
                    p -> Teleport.toWaystone(waystoneData, p)
                );
            } else player.sendMessage(
                new TranslationTextComponent(LangKeys.waystoneNotFound, Colors.wrap(message.waystoneName, Colors.highlight)),
                Util.DUMMY_UUID
            );
        }

    }

    public static final class RequestGui implements PacketHandler.Event<RequestGui.Package> {

        public static final class Package {

            public final Either<String, Info> data;
            public final Optional<PostTile.TilePartInfo> tilePartInfo;

            public Package(Either<String, Info> data, Optional<PostTile.TilePartInfo> tilePartInfo) {
                this.data = data;
                this.tilePartInfo = tilePartInfo;
            }

            public static final class Info {

                public int maxDistance;
                public int distance;
                public boolean isDiscovered;
                public String waystoneName;
                public ItemStack cost;

                public Info(int maxDistance, int distance, boolean isDiscovered, String waystoneName, ItemStack cost) {
                    this.maxDistance = maxDistance;
                    this.distance = distance;
                    this.isDiscovered = isDiscovered;
                    this.waystoneName = waystoneName;
                    this.cost = cost;
                }

                public static final Serializer serializer = new Serializer();
                public static final class Serializer implements BufferSerializable<Info> {

                    @Override
                    public Class<Info> getTargetClass() { return Info.class; }

                    @Override
                    public void write(Info info, PacketBuffer buffer) {
                        buffer.writeInt(info.maxDistance);
                        buffer.writeInt(info.distance);
                        buffer.writeBoolean(info.isDiscovered);
                        buffer.writeString(info.waystoneName);
                        buffer.writeItemStack(info.cost);
                    }

                    @Override
                    public Info read(PacketBuffer buffer) {
                        return new Info(
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readBoolean(),
                            buffer.readString(),
                            buffer.readItemStack()
                        );
                    }
                }
            }
        }

        @Override
        public Class<Package> getMessageClass() {
            return Package.class;
        }

        @Override
        public void encode(Package message, PacketBuffer buffer) {
            Either.BufferSerializer.of(StringSerializer.instance, Package.Info.serializer)
                .write(message.data, buffer);
            PostTile.TilePartInfo.Serializer.optional().write(message.tilePartInfo, buffer);
        }

        @Override
        public Package decode(PacketBuffer buffer) {
            return new Package(
                Either.BufferSerializer.of(StringSerializer.instance, Package.Info.serializer)
                    .read(buffer),
                PostTile.TilePartInfo.Serializer.optional().read(buffer)
            );
        }

        @Override
        public void handle(Package message, NetworkEvent.Context context) {
            if(Config.Client.enableConfirmationScreen.get()) requestOnClient(
                message.data,
                message.tilePartInfo.flatMap(info -> TileEntityUtils.findTileEntity(
                    info.dimensionKey,
                    true,
                    info.pos,
                    PostTile.class
                ).flatMap(tile -> tile.getPart(info.identifier)
                    .flatMap(part -> part.blockPart instanceof SignBlockPart
                        ? Optional.of(new ConfirmTeleportGui.SignInfo(tile, (SignBlockPart) part.blockPart, info, part.offset)) : Optional.empty()
                    ))));
            else message.data.consume(
                l -> Minecraft.getInstance().player.sendStatusMessage(new TranslationTextComponent(l), true),
                r -> PacketHandler.sendToServer(new Request.Package(r.waystoneName))
            );
        }
    }

}
