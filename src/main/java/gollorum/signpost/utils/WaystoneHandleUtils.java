package gollorum.signpost.utils;

import gollorum.signpost.PlayerHandle;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class WaystoneHandleUtils {

    public static Optional<Component> cannotTeleportToBecause(ServerPlayer player, WaystoneHandle dest, String waystoneName) {
        return dest instanceof WaystoneHandle.Vanilla
            ? WaystoneLibrary.getInstance().isDiscovered(new PlayerHandle(player), (WaystoneHandle.Vanilla) dest)
            || !Config.Server.teleport.enforceDiscovery.get()
                ? Optional.empty()
                : Optional.of((Component) new TranslatableComponent(LangKeys.notDiscovered, Colors.wrap(waystoneName, Colors.highlight)))
            : ExternalWaystoneLibrary.getInstance().cannotTeleportToBecause(player, dest);
    }
}
