package gollorum.signpost.utils.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.MapColor;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Serializes a {@link MapColor} by name, e.g. {@code "podzol"}.
 *
 * <p>Minecraft has no registry and no codec for map colours - they are plain constants - so the names are read
 * off {@link MapColor}'s own fields. All three loaders run on official mappings, so those names are the ones
 * the wiki and the vanilla source use.
 */
public class MapColorSerializer {

    private static final Map<String, MapColor> byName = new HashMap<>();
    private static final Map<MapColor, String> names = new HashMap<>();
    static {
        for (var field : MapColor.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != MapColor.class) continue;
            try {
                var color = (MapColor) field.get(null);
                var name = field.getName().toLowerCase(Locale.ROOT);
                byName.put(name, color);
                names.putIfAbsent(color, name);
            } catch (IllegalAccessException ignored) {}
        }
    }

    public static final Codec<MapColor> CODEC = Codec.STRING.comapFlatMap(
        name -> {
            var color = byName.get(name.toLowerCase(Locale.ROOT));
            return color == null
                ? DataResult.error(() -> "Not a valid map color: " + name)
                : DataResult.success(color);
        },
        MapColorSerializer::nameOf
    );

    public static final StreamCodec<ByteBuf, MapColor> STREAM_CODEC =
        ByteBufCodecs.VAR_INT.map(MapColor::byId, color -> color.id);

    private static String nameOf(MapColor color) {
        var name = names.get(color);
        if (name == null) throw new IllegalArgumentException("Unknown map color with id " + color.id);
        return name;
    }

}
