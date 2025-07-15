package gollorum.signpost.utils.serialization;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationSerializer {

    public static final MapCodec<ResourceLocation> MAP_CODEC = ResourceLocation.CODEC
        .fieldOf("ResourceLocation");
}