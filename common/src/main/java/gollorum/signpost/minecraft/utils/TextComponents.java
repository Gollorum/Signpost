package gollorum.signpost.minecraft.utils;

import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.minecraft.gui.utils.Colors;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;


public final class TextComponents {

    public static MutableComponent waystone(ServerPlayer player, String name) {
        return waystone(player, name, true);
    }

    public static MutableComponent waystone(ServerPlayer player, String name, boolean shouldHighlight) {
        MutableComponent ret = shouldHighlight ? Colors.wrap(name, Colors.highlight) : Component.literal(name);
        if(player != null && player.hasPermissions(IConfig.IServer.getInstance().permissions().teleportPermissionLevel()))
            ret.setStyle(ret.getStyle()
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/signpost teleport \"" + name + "\""))
                .withUnderlined(true)
            );
        return ret;
    }

}
