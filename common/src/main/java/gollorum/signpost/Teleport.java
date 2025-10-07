package gollorum.signpost;

import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.compat.ExternalWaystone;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.gui.ConfirmTeleportGui;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.Inventory;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.IDelay;
import gollorum.signpost.utils.Either;
import gollorum.signpost.utils.WaystoneHandleUtils;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.serialization.ComponentCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Teleport {

    public static void toWaystone(WaystoneHandle waystone, ServerPlayer player) {
        if(waystone instanceof WaystoneHandle.Vanilla) {
            WaystoneLocationData waystoneData = WaystoneLibrary.getInstance().getLocationData((WaystoneHandle.Vanilla) waystone);
            toWaystone(waystoneData, player);
        } else Signpost.LOGGER.error("Tried to teleport to non-vanilla waystone " + ((ExternalWaystone.Handle)waystone).modMark());
    }

    public static void toWaystone(WaystoneLocationData waystoneData, ServerPlayer player){
        waystoneData.block().world().mapLeft(Optional::of)
            .leftOr(i -> TileEntityUtils.findWorld(i, false))
        .ifPresent(unspecificWorld -> {
            if(!(unspecificWorld instanceof ServerLevel)) return;
            ServerLevel world = (ServerLevel) unspecificWorld;
            Vector3 location = waystoneData.spawn();
            Vector3 diff = Vector3.fromBlockPos(waystoneData.block().blockPos()).add(new Vector3(0.5f, 0.5f, 0.5f))
                .subtract(location.withY(y -> y + player.getEyeHeight()));
            Angle yaw = Angle.between(
                0, 1,
                diff.x(), diff.z()
            );
            Angle pitch = Angle.fromRadians((float) (Math.PI / 2 + Math.atan(Math.sqrt(diff.x() * diff.x() + diff.z() * diff.z()) / diff.y())));
            Level oldWorld = player.level();
            BlockPos oldPos = player.blockPosition();
            // handle different dimensions outside GUI in case of external waystones
            if (!player.level().dimensionType().equals(world.dimensionType())) {
                if (!(IConfig.getInstance().getServer().teleport().enableAcrossDimensions())) {
                    player.sendSystemMessage(Component.translatable(LangKeys.differentDimension));
                    return;
                }
            }
            Entity toTeleport = player;
            if(IConfig.getInstance().getServer().teleport().allowVehicle()) {
                while(toTeleport.isPassenger()) toTeleport = toTeleport.getVehicle();
            }
            teleportWithChildren(toTeleport, world, location, yaw, pitch);
//            if (IConfig.getInstance().getServer().teleport().allowVehicle() && player.isPassenger()) {
//                Entity vehicle = player.getVehicle();
//                while(vehicle.isPassenger()) vehicle = vehicle.getVehicle();
//                if (!vehicle.level().dimensionType().equals(world.dimensionType())) {
//                    changeDimensionWithChildren(vehicle, world, portalInfo);
//                } else {
//                    var leashedMobs = findLeashedMobs(player);
//                    vehicle.teleportTo(world, location.x, location.y, location.z, RelativeMovement.ALL, yaw.degrees(), pitch.degrees());
//    //                    changeDimensionWithChildren(vehicle, world, portalInfo);
////                    vehicle.setYRot(yaw.degrees());
////                    vehicle.setXRot(pitch.degrees());
////                    vehicle.teleportTo(location.x, location.y, location.z);
//                    if(IConfig.getInstance().getServer().teleport().allowLead())
//                        teleportLeadedAnimals(player, leashedMobs, world, portalInfo);
//                    else unleash(leashedMobs);
//                }
//            } else {
//                var leashedMobs = findLeashedMobs(player);
//                player.teleportTo(world, location.x, location.y, location.z, yaw.degrees(), pitch.degrees());
//
//                if(IConfig.getInstance().getServer().teleport().allowLead())
//                    teleportLeadedAnimals(player, leashedMobs, world, portalInfo);
//                else unleash(leashedMobs);
//
//            }

            final int steps = 6;
            TriConsumer<Level, BlockPos, Float> playStepSound = (soundWorld, pos, volume) -> {
                SoundType soundType = Blocks.STONE.defaultBlockState().getSoundType();
                soundWorld.playSound(null, pos, soundType.getStepSound(), player.getSoundSource(), soundType.getVolume() * volume, soundType.getPitch());
            };
            AtomicReference<Consumer<Integer>> playStepSounds = new AtomicReference<>();
            playStepSounds.set(countdown -> {
                float volume = countdown / (float) steps;
                playStepSound.accept(oldWorld, oldPos, volume);
                if(countdown > 1) IDelay.onServerForFrames(10, () -> playStepSounds.get().accept(countdown - 1));
            });
            playStepSounds.get().accept(steps);
        });
    }

    private static <T extends Entity> T teleportWithChildren(T entity, ServerLevel level, Vector3 pos, Angle yaw, Angle pitch) {
        var passengers = List.copyOf(entity.getPassengers());

        var leashed = findLeashedMobs(entity);

        var changesDimension = entity.level() != level;

        if(entity instanceof ServerPlayer)
            entity.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), yaw.degrees(), pitch.degrees(), true);
        else if (changesDimension) {
            entity.unRide();
            Entity newCopy = entity.getType().create(level, EntitySpawnReason.DIMENSION_TRAVEL);
            if (newCopy == null) {
                return entity;
            }
            newCopy.restoreFrom(entity);
            newCopy.moveOrInterpolateTo(pos.asVec3(), yaw.degrees(), pitch.degrees());
            newCopy.setYHeadRot(yaw.degrees());
            entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
            level.addDuringTeleport(newCopy);
            entity = (T) newCopy;
        } else {
            entity.moveOrInterpolateTo(pos.asVec3(), yaw.degrees(), pitch.degrees());
            entity.setYHeadRot(yaw.degrees());
        }

        var entity2 = entity;

        if(IConfig.getInstance().getServer().teleport().allowLead())
            teleportLeadedAnimals(entity2, leashed, level, pos, yaw, pitch);
        else unleash(leashed);

        for(var p : passengers) {
            var p2 = teleportWithChildren(p, level, pos, yaw, pitch);
            if(changesDimension || p instanceof ServerPlayer) IDelay.onServerForFrames(5, () -> p2.startRiding(entity2, true));
            else entity2.positionRider(p2);
        }
        return entity2;
    }

    private static List<Mob> findLeashedMobs(Entity player) {
        var searchBox = new AABB(player.blockPosition()).inflate(7.0d);
        return player.level().getEntitiesOfClass(Mob.class, searchBox, mob -> mob.getLeashHolder() == player);
    }

    private static void teleportLeadedAnimals(Entity player, List<Mob> leashed, ServerLevel level, Vector3 pos, Angle yaw, Angle pitch) {
        for(Mob mob : leashed) {
            if(level != mob.level())
                mob = teleportWithChildren(mob, level, pos, yaw, pitch);
            else {
                mob.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), yaw.degrees(), pitch.degrees(), true);
            }
            var mob2 = mob;
            IDelay.onServerForFrames(5, () -> mob2.setLeashedTo(player, true));
        }
    }

    private static void unleash(List<Mob> leashed) {
        for(Mob mob : leashed) mob.dropLeash();
    }

    public static void requestOnClient(
        Either<String, RequestGui.Package.Info> data,
        Optional<ConfirmTeleportGui.SignInfo> signInfo
    ) {
        ConfirmTeleportGui.display(data, signInfo);
    }

    public static ItemStack getCost(ServerPlayer player, Vector3 from, Vector3 to) {
        var item = player.server.registryAccess().lookup(Registries.ITEM).flatMap(
            registry -> registry.get(ResourceKey.create(Registries.ITEM, ResourceLocation.parse(IConfig.getInstance().getServer().teleport().costItem())))
        ).map(Holder.Reference::value).orElse(null);
        if(item == null || item.equals(Items.AIR) || player.isCreative() || player.isSpectator()) return ItemStack.EMPTY;
        int distancePerPayment = IConfig.getInstance().getServer().teleport().distancePerPayment();
        int distanceDependentCost = distancePerPayment < 0
            ? 0
            : (int)(from.distanceTo(to) / distancePerPayment);
        return new ItemStack(item, IConfig.getInstance().getServer().teleport().constantPayment() + distanceDependentCost);
    }

    public static final class Request implements PacketHandler.Event.ForServer<Request.Package> {

        public record Package(String waystoneName, Optional<WaystoneHandle> handle) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Package> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Package::waystoneName,
                ByteBufCodecs.optional(WaystoneHandle.STREAM_CODEC), Package::handle,
                Package::new
            );
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Package> codec() {
            return Package.STREAM_CODEC;
        }

        @Override
        public Class<Package> getMessageClass() {
            return Package.class;
        }

        @Override
        public void handle(
            Package message, PacketHandler.Context.Server context
        ) {
            ServerPlayer player = context.sender();
            Optional<WaystoneHandle> waystone = message.handle.or(() -> WaystoneLibrary.getInstance().getHandleByName(message.waystoneName));
            Optional<WaystoneDataBase> data = waystone.flatMap(WaystoneLibrary.getInstance()::getData);
            if(data.isPresent()) {
                WaystoneHandle handle = waystone.get();
                WaystoneLocationData waystoneData = data.get().loc();

                Optional<Component> cannotTeleportBecause = WaystoneHandleUtils.cannotTeleportToBecause(player, handle, message.waystoneName);
                int distance = (int) waystoneData.spawn().distanceTo(Vector3.fromVec3d(player.position()));
                int maxDistance = IConfig.getInstance().getServer().teleport().maximumDistance();
                boolean isTooFarAway = maxDistance > 0 && distance > maxDistance;
                cannotTeleportBecause.ifPresent(player::sendSystemMessage);
                if(isTooFarAway) player.sendSystemMessage(Component.translatable(LangKeys.tooFarAway, Integer.toString(distance), Integer.toString(maxDistance)));
                if(cannotTeleportBecause.isPresent() || isTooFarAway) return;

                Inventory.tryPay(
                    player,
                    Teleport.getCost(player, Vector3.fromVec3d(player.position()), waystoneData.spawn()),
                    p -> Teleport.toWaystone(waystoneData, p)
                );
            } else player.sendSystemMessage(
                Component.translatable(LangKeys.waystoneNotFound, Colors.wrap(message.waystoneName, Colors.highlight))
            );
        }

    }

    public static final class RequestGui implements PacketHandler.Event<RequestGui.Package> {

        public record Package(Either<String, Info> data, Optional<PostTile.TilePartInfo> tilePartInfo) {

            public static final StreamCodec<RegistryFriendlyByteBuf, Package> STREAM_CODEC = StreamCodec.composite(
                Either.streamCodec(ByteBufCodecs.STRING_UTF8, Info.STREAM_CODEC), Package::data,
                ByteBufCodecs.optional(PostTile.TilePartInfo.STREAM_CODEC), Package::tilePartInfo,
                Package::new
            );

            public record Info(
                int maxDistance,
                int distance,
                Optional<Component> cannotTeleportBecause,
                String waystoneName,
                ItemStack cost,
                Optional<WaystoneHandle> handle
            ) {

                public static final StreamCodec<RegistryFriendlyByteBuf, Info> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.INT, Info::maxDistance,
                    ByteBufCodecs.INT, Info::distance,
                    ByteBufCodecs.optional(ComponentCodec.instance), Info::cannotTeleportBecause,
                    ByteBufCodecs.STRING_UTF8, Info::waystoneName,
                    ItemStack.OPTIONAL_STREAM_CODEC, Info::cost,
                    ByteBufCodecs.optional(WaystoneHandle.STREAM_CODEC), Info::handle,
                    Info::new
                );
            }
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Package> codec() {
            return Package.STREAM_CODEC;
        }

        @Override
        public Class<Package> getMessageClass() {
            return Package.class;
        }

        @Override
        public void handle(Package message, PacketHandler.Context context) {
            if(IConfig.getInstance().getClient().enableConfirmationScreen()) requestOnClient(
                message.data,
                message.tilePartInfo.flatMap(info -> TileEntityUtils.findTileEntity(
                    info.dimensionKey,
                    true,
                    info.pos,
                    PostTile.getBlockEntityType()
                ).flatMap(tile -> tile.getPart(info.identifier)
                    .flatMap(part -> part.blockPart() instanceof SignBlockPart
                        ? Optional.of(new ConfirmTeleportGui.SignInfo(tile, (SignBlockPart) part.blockPart(), info, part.offset())) : Optional.empty()
                    ))));
            else message.data.consume(
                l -> Minecraft.getInstance().player.displayClientMessage(Component.translatable(l), true),
                r -> PacketHandler.getInstance().sendToServer(new Request.Package(r.waystoneName, r.handle))
            );
        }
    }

}
