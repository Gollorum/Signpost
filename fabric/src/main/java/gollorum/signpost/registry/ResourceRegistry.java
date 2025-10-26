package gollorum.signpost.registry;

import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;

import java.util.HashSet;
import java.util.Set;

//import static net.neoforged.fml.common.Mod.EventBusSubscriber.Bus.MOD;

//@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Signpost.MOD_ID, bus = MOD)
public class ResourceRegistry {

    private static final Set<Class<? extends SignBlockPart>> signTypesToHandle = new HashSet<>();
    static {
        signTypesToHandle.add(SmallWideSignBlockPart.class);
        signTypesToHandle.add(SmallShortSignBlockPart.class);
        signTypesToHandle.add(LargeSignBlockPart.class);
    }

//    @SubscribeEvent
//    static void onTextureStitch(TextureStitchEvent.Pre event) {
//        if(event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS))
//            for(ResourceLocation texture: Overlay.getAllOverlays().stream().flatMap(o ->
//                signTypesToHandle.stream().map(o::textureFor)
//            ).toList())
//                event.addSprite(texture);
//    }

}
