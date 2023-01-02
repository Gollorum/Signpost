package gollorum.signpost.utils;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.LangKeys;
import gollorum.signpost.minecraft.utils.TileEntityUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class WaystoneHandleUtils {

    public static Optional<Component> cannotTeleportToBecause(ServerPlayer player, WaystoneHandle dest, String waystoneName) {
        if (dest instanceof WaystoneHandle.Vanilla) {
            AtomicReference<Optional<Component>> reason = new AtomicReference<>(Optional.empty());
            boolean isNotDiscoveredAndEnforceDiscovery = !(WaystoneLibrary.getInstance().isDiscovered(new PlayerHandle(player), (WaystoneHandle.Vanilla) dest)) && Config.Server.teleport.enforceDiscovery.get();

            if (isNotDiscoveredAndEnforceDiscovery) {
                reason.set(Optional.of((Component) new TranslatableComponent(LangKeys.notDiscovered, Colors.wrap(waystoneName, Colors.highlight))));
            }

            WaystoneLibrary.getInstance().getLocationData((WaystoneHandle.Vanilla) dest).block.world.mapLeft(Optional::of)
                    .leftOr(i -> TileEntityUtils.findWorld(i, false))
                    .ifPresent(world -> {
                        boolean isAcrossDimensionsAndEnforceDimension = !(player.level.dimension().equals(world.dimension())) && !(Config.Server.teleport.enableAcrossDimensions.get());

                        if (isAcrossDimensionsAndEnforceDimension) {
                            reason.set(Optional.of((Component) new TranslatableComponent(LangKeys.differentDimension)));
                        }
                    });
            return reason.get();
        }
        return ExternalWaystoneLibrary.getInstance().cannotTeleportToBecause(player, dest);
    }
}
