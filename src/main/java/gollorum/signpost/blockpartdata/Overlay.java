package gollorum.signpost.blockpartdata;

import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.utils.tints.FoliageTint;
import gollorum.signpost.minecraft.utils.tints.GrassTint;
import gollorum.signpost.utils.Tint;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

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

    public abstract ResourceLocation textureFor(Class<? extends SignBlockPart> signClass);

    private static <T> T logErrorAndReturn(String error, T t) {
        Signpost.LOGGER.error(error);
        return t;
    }

    public static final Overlay Gras = new Overlay(Optional.of(new GrassTint()), "gras") {
        @Override
        public ResourceLocation textureFor(Class<? extends SignBlockPart> signClass) {
            return signClass.equals(SmallWideSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_grass")
                : signClass.equals(SmallShortSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_grass_short")
                : signClass.equals(LargeSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_grass_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, textureFor(SmallWideSignBlockPart.class));
        }
    };

    public static final Overlay Vine = new Overlay(Optional.of(new FoliageTint()), "vine") {
        @Override
        public ResourceLocation textureFor(Class<? extends SignBlockPart> signClass) {
            return signClass.equals(SmallWideSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_vine")
                : signClass.equals(SmallShortSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_vine_short")
                : signClass.equals(LargeSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_vine_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, textureFor(SmallWideSignBlockPart.class));
        }
    };

    public static final Overlay Snow = new Overlay(Optional.empty(), "snow") {
        @Override
        public ResourceLocation textureFor(Class<? extends SignBlockPart> signClass) {
            return signClass.equals(SmallWideSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_snow")
                : signClass.equals(SmallShortSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_snow_short")
                : signClass.equals(LargeSignBlockPart.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_snow_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, textureFor(SmallWideSignBlockPart.class));
        }
    };

    static {
        register(Gras);
        register(Vine);
        register(Snow);
    }

    public static final CompoundSerializable<Overlay> Serializer = new SerializerImpl();
    public static final class SerializerImpl implements CompoundSerializable<Overlay> {
        @Override
        public Class<Overlay> getTargetClass() {
            return Overlay.class;
        }

        @Override
        public CompoundTag write(Overlay overlay, CompoundTag compound) {
            compound.putString("Id", overlay.id);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundTag compound) {
            return compound.contains("Id");
        }

        @Override
        public Overlay read(CompoundTag compound) {
            String id = compound.getString("Id");
            if(!overlayRegistry.containsKey(id)) {
                Signpost.LOGGER.error("Tried to read overlay with id " + id + ", but it was not registered.");
                return Gras;
            } else return overlayRegistry.get(id);
        }
    };

}
