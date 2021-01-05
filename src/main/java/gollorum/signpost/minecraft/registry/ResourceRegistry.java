package gollorum.signpost.minecraft.registry;

import gollorum.signpost.signdata.Overlay;
import gollorum.signpost.signdata.types.LargeSign;
import gollorum.signpost.signdata.types.Sign;
import gollorum.signpost.signdata.types.SmallShortSign;
import gollorum.signpost.signdata.types.SmallWideSign;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceRegistry {

    private static final Set<Class<? extends Sign>> signTypesToHandle = new HashSet<>();
    static {
        signTypesToHandle.add(SmallWideSign.class);
        signTypesToHandle.add(SmallShortSign.class);
        signTypesToHandle.add(LargeSign.class);
    }

    @SubscribeEvent
    static void onTextureStitch(TextureStitchEvent.Pre event) {
        if(event.getMap().getTextureLocation().equals(PlayerContainer.LOCATION_BLOCKS_TEXTURE))
            for(ResourceLocation texture: Overlay.getAllOverlays().stream().flatMap(o ->
                signTypesToHandle.stream().map(o::textureFor)
            ).collect(Collectors.toList()))
                event.addSprite(texture);
    }

    public static void register(IEventBus bus) { bus.register(ResourceRegistry.class); }

}
