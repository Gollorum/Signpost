package gollorum.signpost.compat;

import gollorum.signpost.networking.PacketHandler;
import hunternif.mc.impl.atlas.AntiqueAtlasModClient;
import hunternif.mc.impl.atlas.client.gui.GuiAtlas;
import hunternif.mc.impl.atlas.event.MarkerClickedCallback;
import hunternif.mc.impl.atlas.marker.Marker;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

public class AntiqueAtlasClickListener implements MarkerClickedCallback {

    // I am sorry.
    private static Field hoveredMarkerField = null;
    private static Field getHoveredMarkerField() {
        if(hoveredMarkerField == null) {
            try {
                hoveredMarkerField = GuiAtlas.class.getDeclaredField("hoveredMarker");
                hoveredMarkerField.setAccessible(true);
            } catch (Exception ignored) {}
        }
        return hoveredMarkerField;
    }

    @Override
    public boolean onClicked(Player player, Marker marker, int i) {
        if(marker.isGlobal() && marker.getType().equals(AntiqueAtlasAdapter.markerType)) {
            AntiqueAtlasModClient.getAtlasGUI().onClose();

            // Again, I am sorry.
            try {
                getHoveredMarkerField().set(AntiqueAtlasModClient.getAtlasGUI(), null);
            } catch (Exception ignored) {}

            PacketHandler.sendToServer(new RequestTeleportByMarkerEvent.Packet(marker.getId()));
            return true;
        }
        return false;
    }

}
