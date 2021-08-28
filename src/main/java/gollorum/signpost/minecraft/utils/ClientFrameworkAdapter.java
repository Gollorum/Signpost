package gollorum.signpost.minecraft.utils;

import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;

public class ClientFrameworkAdapter {

    public static void showStatusMessage(TranslationTextComponent message, boolean inActionBar) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if(player == null) Signpost.LOGGER.error("Client player was null, failed to show status message");
        else player.displayClientMessage(message, inActionBar);
    }

}
