package gollorum.signpost.blockpartdata;

import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.LargeSign;
import gollorum.signpost.blockpartdata.types.Sign;
import gollorum.signpost.blockpartdata.types.SmallShortSign;
import gollorum.signpost.blockpartdata.types.SmallWideSign;
import gollorum.signpost.minecraft.registry.ColorRegistry;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    public int getTintAt(World world, BlockPos pos) {
        return ColorRegistry.getOverlayColor(tintIndex, world, pos);
    }

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
        public CompoundNBT write(Overlay overlay, CompoundNBT compound) {
            compound.putString("Id", overlay.id);
            return compound;
        }

        @Override
        public boolean isContainedIn(CompoundNBT compound) {
            return compound.contains("Id");
        }

        @Override
        public Overlay read(CompoundNBT compound) {
            String id = compound.getString("Id");
            if(!overlayRegistry.containsKey(id)) {
                Signpost.LOGGER.error("Tried to read overlay with id " + id + ", but it was not registered.");
                return Gras;
            } else return overlayRegistry.get(id);
        }
    };

}
