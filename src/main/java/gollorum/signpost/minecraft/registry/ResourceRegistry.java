package gollorum.signpost.minecraft.registry;

import gollorum.signpost.minecraft.block.Post;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ResourceRegistry {

    private static final ResourceLocation[] additionalTexturesToStitch = Post.OverlayTextures.All;

    @SubscribeEvent
    static void onTextureStitch(TextureStitchEvent.Pre event) {
        if(event.getMap().getTextureLocation().equals(PlayerContainer.LOCATION_BLOCKS_TEXTURE))
            for(ResourceLocation texture: additionalTexturesToStitch) event.addSprite(texture);
    }

    public static void register(IEventBus bus) { bus.register(ResourceRegistry.class); }

}
