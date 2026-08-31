package gollorum.signpost.utils.serialization;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

public class ResourceLocationSerializer {

    public static final MapCodec<Identifier> MAP_CODEC = Identifier.CODEC
        .fieldOf("ResourceLocation");
}