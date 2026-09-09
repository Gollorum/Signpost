package gollorum.signpost.minecraft.utils;

import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class ClientFrameworkAdapter {

    public static void showStatusMessage(Component message, boolean inActionBar) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) Signpost.LOGGER.error("Client sender was null, failed to show status message");
        else if(inActionBar) player.sendOverlayMessage(message);
        else player.sendSystemMessage(message);
    }

}
