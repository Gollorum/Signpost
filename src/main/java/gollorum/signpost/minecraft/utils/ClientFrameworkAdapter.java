package gollorum.signpost.minecraft.utils;

import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TranslatableComponent;

public class ClientFrameworkAdapter {

    public static void showStatusMessage(TranslatableComponent message, boolean inActionBar) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) Signpost.LOGGER.error("Client player was null, failed to show status message");
        else player.displayClientMessage(message, inActionBar);
    }

}
