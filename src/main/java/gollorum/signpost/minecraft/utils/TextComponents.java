package gollorum.signpost.minecraft.utils;

import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.minecraft.gui.utils.Colors;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;

import javax.annotation.Nullable;

public final class TextComponents {

    public static ITextComponent waystone(@Nullable ServerPlayerEntity player, String name) {
        return waystone(player, name, true);
    }

    public static ITextComponent waystone(@Nullable ServerPlayerEntity player, String name, boolean shouldHighlight) {
        ITextComponent ret = shouldHighlight ? Colors.wrap(name, Colors.highlight) : new StringTextComponent(name);
        if(player != null && player.hasPermissions(Config.Server.permissions.teleportPermissionLevel.get()))
            ret.setStyle(ret.getStyle()
                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/signpost teleport \"" + name + "\""))
                .setUnderlined(true)
            );
        return ret;
    }

}
