package gollorum.signpost.blockpartdata;

import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.registry.ColorRegistry;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.FoliageColors;
import net.minecraft.world.GrassColors;
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

    public abstract ResourceLocation textureFor(Class<? extends SignBlockPart> signClass);

    public int getTintAt(World world, BlockPos pos) {
        return ColorRegistry.getOverlayColor(tintIndex, world, pos);
    }

    public int getDefaultTint() {
        switch (tintIndex) {
            case GrasTint: return GrassColors.get(0.8, 0.4);
            case FoliageTint: return FoliageColors.getDefaultColor();
            case WaterTint: return 0x3F76E4;
            case NoTint:
            default: return 0xffffff;
        }
    }

    private static <T> T logErrorAndReturn(String error, T t) {
        Signpost.LOGGER.error(error);
        return t;
    }

    public static final Overlay Gras = new Overlay(GrasTint, "gras") {
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

    public static final Overlay Vine = new Overlay(FoliageTint, "vine") {
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

    public static final Overlay Snow = new Overlay(NoTint, "snow") {
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
