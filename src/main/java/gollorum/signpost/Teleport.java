package gollorum.signpost;

import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.compat.ExternalWaystone;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.ConfirmTeleportGui;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.Inventory;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.WaystoneHandleUtils;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.ComponentSerializer;
import gollorum.signpost.utils.serialization.StringSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class Teleport {

    public static void toWaystone(WaystoneHandle waystone, ServerPlayer player) {
        if(waystone instanceof WaystoneHandle.Vanilla) {
            WaystoneLocationData waystoneData = WaystoneLibrary.getInstance().getLocationData((WaystoneHandle.Vanilla) waystone);
            toWaystone(waystoneData, player);
        } else Signpost.LOGGER.error("Tried to teleport to non-vanilla waystone " + ((ExternalWaystone.Handle)waystone).modMark());
    }

    public static void toWaystone(WaystoneLocationData waystoneData, ServerPlayer player){
        waystoneData.block.world.mapLeft(Optional::of)
            .leftOr(i -> TileEntityUtils.findWorld(i, false))
        .ifPresent(unspecificWorld -> {
            if(!(unspecificWorld instanceof ServerLevel)) return;
            ServerLevel world = (ServerLevel) unspecificWorld;
            Vector3 location = waystoneData.spawn;
            Vector3 diff = Vector3.fromBlockPos(waystoneData.block.blockPos).add(new Vector3(0.5f, 0.5f, 0.5f))
                .subtract(location.withY(y -> y + player.getEyeHeight()));
            Angle yaw = Angle.between(
                0, 1,
                diff.x, diff.z
            );
            Angle pitch = Angle.fromRadians((float) (Math.PI / 2 + Math.atan(Math.sqrt(diff.x * diff.x + diff.z * diff.z) / diff.y)));
            Level oldWorld = player.level();
            BlockPos oldPos = player.blockPosition();
            // Handle different dimensions outside GUI in case of external waystones
            if (!player.level().dimensionType().equals(world.dimensionType())) {
                if (!(Config.Server.teleport.enableAcrossDimensions.get())) {
                    player.sendSystemMessage(Component.translatable(LangKeys.differentDimension));
                    return;
                }
//                player.changeDimension(world, new ITeleporter() {});
            }
            var teleporter = new ITeleporter() {
                @Override
                public @Nullable PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                    return new PortalInfo(
                        location.asVec3(),
                        Vec3.ZERO,
                        yaw.degrees(),
                        pitch.degrees()
                    );
                }
            };
            if (Config.Server.teleport.allowVehicle.get() && player.isPassenger()) {
                Entity vehicle = player.getVehicle();
                while(vehicle.isPassenger()) vehicle = vehicle.getVehicle();
                if (!player.level().dimensionType().equals(world.dimensionType())) {
                    changeDimensionWithChildren(vehicle, world, teleporter);
                } else {
                    var leashedMobs = findLeashedMobs(player);
                    vehicle.setYRot(yaw.degrees());
                    vehicle.setXRot(pitch.degrees());
                    vehicle.teleportTo(location.x, location.y, location.z);
                    if(Config.Server.teleport.allowLead.get())
                        teleportLeadedAnimals(player, leashedMobs, world, teleporter);
                    else unleash(leashedMobs);
                }
            } else {
                var leashedMobs = findLeashedMobs(player);
                player.teleportTo(world, location.x, location.y, location.z, yaw.degrees(), pitch.degrees());

                if(Config.Server.teleport.allowLead.get())
                    teleportLeadedAnimals(player, leashedMobs, world, teleporter);
                else unleash(leashedMobs);

            }

            final int steps = 6;
            TriConsumer<Level, BlockPos, Float> playStepSound = (soundWorld, pos, volume) -> {
                SoundType soundType = Blocks.STONE.defaultBlockState().getSoundType();
                soundWorld.playSound(null, pos, soundType.getStepSound(), player.getSoundSource(), soundType.getVolume() * volume, soundType.getPitch());
            };
            AtomicReference<Consumer<Integer>> playStepSounds = new AtomicReference<>();
            playStepSounds.set(countdown -> {
                float volume = countdown / (float) steps;
                playStepSound.accept(oldWorld, oldPos, volume);
                if(countdown > 1) Delay.onServerForFrames(15, () -> playStepSounds.get().accept(countdown - 1));
            });
            playStepSounds.get().accept(steps);
        });
    }

    private static <T extends Entity> T changeDimensionWithChildren(T entity, ServerLevel level, ITeleporter tp) {
        var passengers = List.copyOf(entity.getPassengers());

        var leashed = findLeashedMobs(entity);

        var newCopy = entity.changeDimension(level, tp);
        if(newCopy == null) return entity;

        if(Config.Server.teleport.allowLead.get())
            teleportLeadedAnimals(newCopy, leashed, level, tp);
        else unleash(leashed);

        for(var p : passengers) {
            var p2 = changeDimensionWithChildren(p, level, tp);
            Delay.onServerForFrames(5, () -> p2.startRiding(newCopy, true));
        }
        return (T) newCopy;
    }

    private static List<Mob> findLeashedMobs(Entity player) {
        var searchBox = new AABB(player.blockPosition()).inflate(7.0d);
        return player.level().getEntitiesOfClass(Mob.class, searchBox, mob -> mob.getLeashHolder() == player);
    }

    private static void teleportLeadedAnimals(Entity player, List<Mob> leashed, ServerLevel level, ITeleporter tp) {
        for(Mob mob : leashed) {
            if(level != mob.level())
                mob = changeDimensionWithChildren(mob, level, tp);
            else {
                var portalIngo = tp.getPortalInfo(mob, level, null);
                mob.setYRot(portalIngo.yRot);
                mob.setXRot(portalIngo.xRot);
                mob.teleportTo(portalIngo.pos.x, portalIngo.pos.y, portalIngo.pos.z);
            }
            var mob2 = mob;
            Delay.onServerForFrames(5, () -> mob2.setLeashedTo(player, true));
        }
    }

    private static void unleash(List<Mob> leashed) {
        for(Mob mob : leashed) mob.dropLeash(true, true);
    }

    public static void requestOnClient(
        Either<String, RequestGui.Package.Info> data,
        Optional<ConfirmTeleportGui.SignInfo> signInfo
    ) {
        ConfirmTeleportGui.display(data, signInfo);
    }

    public static ItemStack getCost(Player player, Vector3 from, Vector3 to) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Config.Server.teleport.costItem.get()));
        if(item == null || item.equals(Items.AIR) || player.isCreative() || player.isSpectator()) return ItemStack.EMPTY;
        int distancePerPayment = Config.Server.teleport.distancePerPayment.get();
        int distanceDependentCost = distancePerPayment < 0
            ? 0
            : (int)(from.distanceTo(to) / distancePerPayment);
        return new ItemStack(item, Config.Server.teleport.constantPayment.get() + distanceDependentCost);
    }

    public static final class Request implements PacketHandler.Event<Request.Package> {

        public static final class Package {
            public final String waystoneName;
            public final Optional<WaystoneHandle> handle;
            public Package(
                String waystoneName, Optional<WaystoneHandle> handle
            ) {
                this.waystoneName = waystoneName;
                this.handle = handle;
            }
        }

        @Override
        public Class<Package> getMessageClass() {
            return Package.class;
        }

        @Override
        public void encode(Package message, FriendlyByteBuf buffer) {
            StringSerializer.instance.write(message.waystoneName, buffer);
            buffer.writeBoolean(message.handle.isPresent());
            message.handle.ifPresent(h -> h.write(buffer));
        }

        @Override
        public Package decode(FriendlyByteBuf buffer) {
            return new Package(StringSerializer.instance.read(buffer), buffer.readBoolean() ? WaystoneHandle.read(buffer) : Optional.empty());
        }

        @Override
        public void handle(
            Package message, NetworkEvent.Context context
        ) {
            ServerPlayer player = context.getSender();
            Optional<WaystoneHandle> waystone = message.handle.or(() -> WaystoneLibrary.getInstance().getHandleByName(message.waystoneName));
            Optional<WaystoneDataBase> data = waystone.flatMap(WaystoneLibrary.getInstance()::getData);
            if(data.isPresent()) {
                WaystoneHandle handle = waystone.get();
                WaystoneLocationData waystoneData = data.get().loc();

                Optional<Component> cannotTeleportBecause = WaystoneHandleUtils.cannotTeleportToBecause(player, handle, message.waystoneName);
                int distance = (int) waystoneData.spawn.distanceTo(Vector3.fromVec3d(player.position()));
                int maxDistance = Config.Server.teleport.maximumDistance.get();
                boolean isTooFarAway = maxDistance > 0 && distance > maxDistance;
                cannotTeleportBecause.ifPresent(player::sendSystemMessage);
                if(isTooFarAway) player.sendSystemMessage(Component.translatable(LangKeys.tooFarAway, Integer.toString(distance), Integer.toString(maxDistance)));
                if(cannotTeleportBecause.isPresent() || isTooFarAway) return;

                Inventory.tryPay(
                    player,
                    Teleport.getCost(player, Vector3.fromVec3d(player.position()), waystoneData.spawn),
                    p -> Teleport.toWaystone(waystoneData, p)
                );
            } else player.sendSystemMessage(
                Component.translatable(LangKeys.waystoneNotFound, Colors.wrap(message.waystoneName, Colors.highlight))
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

                public final int maxDistance;
                public final int distance;
                public final Optional<Component> cannotTeleportBecause;
                public final String waystoneName;
                public final ItemStack cost;
                public final Optional<WaystoneHandle> handle;

                public Info(int maxDistance, int distance, Optional<Component> cannotTeleportBecause, String waystoneName, ItemStack cost, Optional<WaystoneHandle> handle) {
                    this.maxDistance = maxDistance;
                    this.distance = distance;
                    this.cannotTeleportBecause = cannotTeleportBecause;
                    this.waystoneName = waystoneName;
                    this.cost = cost;
                    this.handle = handle;
                }

                public static final Serializer serializer = new Serializer();
                public static final class Serializer implements BufferSerializable<Info> {

                    @Override
                    public Class<Info> getTargetClass() { return Info.class; }

                    @Override
                    public void write(Info info, FriendlyByteBuf buffer) {
                        buffer.writeInt(info.maxDistance);
                        buffer.writeInt(info.distance);
                        ComponentSerializer.instance.optional().write(info.cannotTeleportBecause, buffer);
                        StringSerializer.instance.write(info.waystoneName, buffer);
                        buffer.writeItem(info.cost);
                        buffer.writeOptional(info.handle, (b, h) -> h.write(b));
                    }

                    @Override
                    public Info read(FriendlyByteBuf buffer) {
                        return new Info(
                            buffer.readInt(),
                            buffer.readInt(),
                            ComponentSerializer.instance.optional().read(buffer),
                            StringSerializer.instance.read(buffer),
                            buffer.readItem(),
                            buffer.readOptional(WaystoneHandle::read).flatMap(o -> o)
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
        public void encode(Package message, FriendlyByteBuf buffer) {
            Either.BufferSerializer.of(StringSerializer.instance, Package.Info.serializer)
                .write(message.data, buffer);
            PostTile.TilePartInfo.Serializer.optional().write(message.tilePartInfo, buffer);
        }

        @Override
        public Package decode(FriendlyByteBuf buffer) {
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
                    PostTile.getBlockEntityType()
                ).flatMap(tile -> tile.getPart(info.identifier)
                    .flatMap(part -> part.blockPart instanceof SignBlockPart
                        ? Optional.of(new ConfirmTeleportGui.SignInfo(tile, (SignBlockPart) part.blockPart, info, part.offset)) : Optional.empty()
                    ))));
            else message.data.consume(
                l -> Minecraft.getInstance().player.displayClientMessage(Component.translatable(l), true),
                r -> PacketHandler.sendToServer(new Request.Package(r.waystoneName, r.handle))
            );
        }
    }

}
