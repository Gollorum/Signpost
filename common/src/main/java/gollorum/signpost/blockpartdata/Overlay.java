package gollorum.signpost.blockpartdata;

import com.mojang.serialization.Codec;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.minecraft.utils.tints.FoliageTint;
import gollorum.signpost.minecraft.utils.tints.GrassTint;
import gollorum.signpost.utils.Tint;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class Overlay {

    private static final Map<String, Overlay> overlayRegistry = new HashMap<>();

    public static void register(Overlay overlay) { overlayRegistry.put(overlay.id, overlay); }
    public static Collection<Overlay> getAllOverlays() { return overlayRegistry.values(); }

    public final Optional<Tint> tint;
    public final String id;

    protected Overlay(Optional<Tint> tint, String id) {
        this.tint = tint;
        this.id = id;
    }

    public abstract SpriteId materialFor(Class<? extends SignBlockPart> signClass);

    private static <T> T logErrorAndReturn(String error, T t) {
        Signpost.LOGGER.error(error);
        return t;
    }

    public static final Overlay Gras = new Overlay(Optional.of(GrassTint.INSTANCE), "gras") {
        @Override
        public SpriteId materialFor(Class<? extends SignBlockPart> signClass) {
            return new SpriteId(
                TextureResource.blockAtlas,
                signClass.equals(SmallWideSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_grass")
                : signClass.equals(SmallShortSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_grass_short")
                : signClass.equals(LargeSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_grass_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, materialFor(SmallWideSignBlockPart.class).texture()));
        }
    };

    public static final Overlay Vine = new Overlay(Optional.of(FoliageTint.INSTANCE), "vine") {
        @Override
        public SpriteId materialFor(Class<? extends SignBlockPart> signClass) {
            return new SpriteId(
                TextureResource.blockAtlas,
                signClass.equals(SmallWideSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_vine")
                : signClass.equals(SmallShortSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_vine_short")
                : signClass.equals(LargeSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_vine_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, materialFor(SmallWideSignBlockPart.class).texture()));
        }
    };

    public static final Overlay Snow = new Overlay(Optional.empty(), "snow") {
        @Override
        public SpriteId materialFor(Class<? extends SignBlockPart> signClass) {
            return new SpriteId(
                TextureResource.blockAtlas,signClass.equals(SmallWideSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_snow")
                : signClass.equals(SmallShortSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_snow_short")
                : signClass.equals(LargeSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_snow_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, materialFor(SmallWideSignBlockPart.class).texture()));
        }
    };

    public static final Overlay Mycelium = new Overlay(Optional.empty(), "mycelium") {
        @Override
        public SpriteId materialFor(Class<? extends SignBlockPart> signClass) {
            return new SpriteId(
                TextureResource.blockAtlas,signClass.equals(SmallWideSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_mycelium")
                : signClass.equals(SmallShortSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_mycelium_short")
                : signClass.equals(LargeSignBlockPart.class)
                ? Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/sign_overlay_mycelium_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, materialFor(SmallWideSignBlockPart.class).texture()));
        }
    };

    static {
        register(Gras);
        register(Vine);
        register(Snow);
        register(Mycelium);
    }

    public static final Codec<Overlay> CODEC = Codec.STRING.xmap(
        id -> {
            if(!overlayRegistry.containsKey(id)) {
                Signpost.LOGGER.error("Tried to read overlay with id " + id + ", but it was not registered.");
                return Gras;
            } else return overlayRegistry.get(id);
        },
        overlay -> overlay.id
    );

    public static final StreamCodec<ByteBuf, Overlay> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
        id -> {
            if(!overlayRegistry.containsKey(id)) {
                Signpost.LOGGER.error("Tried to read overlay with id " + id + ", but it was not registered.");
                return Gras;
            } else return overlayRegistry.get(id);
        },
        overlay -> overlay.id
    );

}
