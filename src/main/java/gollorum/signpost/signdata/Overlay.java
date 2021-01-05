package gollorum.signpost.signdata;

import gollorum.signpost.Signpost;
import gollorum.signpost.signdata.types.LargeSign;
import gollorum.signpost.signdata.types.Sign;
import gollorum.signpost.signdata.types.SmallShortSign;
import gollorum.signpost.signdata.types.SmallWideSign;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Overlay {

    private static final Map<String, Overlay> overlayRegistry = new HashMap<>();

    public static void register(Overlay overlay) { overlayRegistry.put(overlay.id, overlay); }
    public static Collection<Overlay> getAllOverlays() { return overlayRegistry.values(); }

    public static final int NoTint = 0;
    public static final int GrasTint = 1;
    public static final int FoliageTint = 2;
    public static final int WaterTint = 3;

    public final int tintIndex;
    public final String id;

    protected Overlay(int tintIndex, String id) {
        this.tintIndex = tintIndex;
        this.id = id;
    }

    public abstract ResourceLocation textureFor(Class<? extends Sign> signClass);

    private static <T> T logErrorAndReturn(String error, T t) {
        Signpost.LOGGER.error(error);
        return t;
    }

    public static final Overlay Gras = new Overlay(GrasTint, "gras") {
        @Override
        public ResourceLocation textureFor(Class<? extends Sign> signClass) {
            return signClass.equals(SmallWideSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_grass")
                : signClass.equals(SmallShortSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_grass_short")
                : signClass.equals(LargeSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_grass_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, textureFor(SmallWideSign.class));
        }
    };

    public static final Overlay Vine = new Overlay(FoliageTint, "vine") {
        @Override
        public ResourceLocation textureFor(Class<? extends Sign> signClass) {
            return signClass.equals(SmallWideSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_vine")
                : signClass.equals(SmallShortSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_vine_short")
                : signClass.equals(LargeSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_vine_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, textureFor(SmallWideSign.class));
        }
    };

    public static final Overlay Snow = new Overlay(NoTint, "snow") {
        @Override
        public ResourceLocation textureFor(Class<? extends Sign> signClass) {
            return signClass.equals(SmallWideSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_snow")
                : signClass.equals(SmallShortSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_snow_short")
                : signClass.equals(LargeSign.class)
                ? new ResourceLocation(Signpost.MOD_ID, "block/sign_overlay_snow_large")
                : logErrorAndReturn("Sign class " + signClass + " is not supported by " + this, textureFor(SmallWideSign.class));
        }
    };

    static {
        register(Gras);
        register(Vine);
        register(Snow);
    }

    public static final CompoundSerializable<Overlay> Serializer = new CompoundSerializable<Overlay>() {
        @Override
        public void writeTo(Overlay overlay, CompoundNBT compound, String keyPrefix) {
            compound.putString(keyPrefix + "Id", overlay.id);
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound, String keyPrefix) {
            return compound.contains(keyPrefix + "Id");
        }

        @Override
        public Overlay read(CompoundNBT compound, String keyPrefix) {
            String id = compound.getString(keyPrefix + "Id");
            if(!overlayRegistry.containsKey(id)) {
                Signpost.LOGGER.error("Tried to read overlay with id " + id + ", but it was not registered.");
                return Gras;
            } else return overlayRegistry.get(id);
        }
    };

}
