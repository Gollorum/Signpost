package gollorum.signpost.utils.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import gollorum.signpost.Signpost;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Serializes a {@link MapColor} by name, e.g. {@code "podzol"}.
 *
 * <p>Minecraft has no registry and no codec for map colours - they are plain constants - so the names are
 * spelled out here. They have to be: Fabric runs on intermediary mappings outside the dev environment, where
 * reading the names off {@link MapColor}'s fields yields {@code field_16003} rather than {@code WOOD} and
 * every model type fails to parse. Referencing the constants makes the loaders remap them for us instead.
 *
 * <p>The names are the field names lowercased, which is what the wiki and the vanilla source use.
 */
public class MapColorSerializer {

    private static final Map<String, MapColor> byName = new HashMap<>();
    private static final Map<MapColor, String> names = new HashMap<>();

    private static void register(String name, MapColor color) {
        byName.put(name, color);
        names.putIfAbsent(color, name);
    }

    static {
        register("none", MapColor.NONE);
        register("grass", MapColor.GRASS);
        register("sand", MapColor.SAND);
        register("wool", MapColor.WOOL);
        register("fire", MapColor.FIRE);
        register("ice", MapColor.ICE);
        register("metal", MapColor.METAL);
        register("plant", MapColor.PLANT);
        register("snow", MapColor.SNOW);
        register("clay", MapColor.CLAY);
        register("dirt", MapColor.DIRT);
        register("stone", MapColor.STONE);
        register("water", MapColor.WATER);
        register("wood", MapColor.WOOD);
        register("quartz", MapColor.QUARTZ);
        register("color_orange", MapColor.COLOR_ORANGE);
        register("color_magenta", MapColor.COLOR_MAGENTA);
        register("color_light_blue", MapColor.COLOR_LIGHT_BLUE);
        register("color_yellow", MapColor.COLOR_YELLOW);
        register("color_light_green", MapColor.COLOR_LIGHT_GREEN);
        register("color_pink", MapColor.COLOR_PINK);
        register("color_gray", MapColor.COLOR_GRAY);
        register("color_light_gray", MapColor.COLOR_LIGHT_GRAY);
        register("color_cyan", MapColor.COLOR_CYAN);
        register("color_purple", MapColor.COLOR_PURPLE);
        register("color_blue", MapColor.COLOR_BLUE);
        register("color_brown", MapColor.COLOR_BROWN);
        register("color_green", MapColor.COLOR_GREEN);
        register("color_red", MapColor.COLOR_RED);
        register("color_black", MapColor.COLOR_BLACK);
        register("gold", MapColor.GOLD);
        register("diamond", MapColor.DIAMOND);
        register("lapis", MapColor.LAPIS);
        register("emerald", MapColor.EMERALD);
        register("podzol", MapColor.PODZOL);
        register("nether", MapColor.NETHER);
        register("terracotta_white", MapColor.TERRACOTTA_WHITE);
        register("terracotta_orange", MapColor.TERRACOTTA_ORANGE);
        register("terracotta_magenta", MapColor.TERRACOTTA_MAGENTA);
        register("terracotta_light_blue", MapColor.TERRACOTTA_LIGHT_BLUE);
        register("terracotta_yellow", MapColor.TERRACOTTA_YELLOW);
        register("terracotta_light_green", MapColor.TERRACOTTA_LIGHT_GREEN);
        register("terracotta_pink", MapColor.TERRACOTTA_PINK);
        register("terracotta_gray", MapColor.TERRACOTTA_GRAY);
        register("terracotta_light_gray", MapColor.TERRACOTTA_LIGHT_GRAY);
        register("terracotta_cyan", MapColor.TERRACOTTA_CYAN);
        register("terracotta_purple", MapColor.TERRACOTTA_PURPLE);
        register("terracotta_blue", MapColor.TERRACOTTA_BLUE);
        register("terracotta_brown", MapColor.TERRACOTTA_BROWN);
        register("terracotta_green", MapColor.TERRACOTTA_GREEN);
        register("terracotta_red", MapColor.TERRACOTTA_RED);
        register("terracotta_black", MapColor.TERRACOTTA_BLACK);
        register("crimson_nylium", MapColor.CRIMSON_NYLIUM);
        register("crimson_stem", MapColor.CRIMSON_STEM);
        register("crimson_hyphae", MapColor.CRIMSON_HYPHAE);
        register("warped_nylium", MapColor.WARPED_NYLIUM);
        register("warped_stem", MapColor.WARPED_STEM);
        register("warped_hyphae", MapColor.WARPED_HYPHAE);
        register("warped_wart_block", MapColor.WARPED_WART_BLOCK);
        register("deepslate", MapColor.DEEPSLATE);
        register("raw_iron", MapColor.RAW_IRON);
        register("glow_lichen", MapColor.GLOW_LICHEN);
        warnAboutUnnamedColors();
    }

    /**
     * Names every colour Minecraft knows, or says which ones it missed. A colour added by a later Minecraft
     * version would otherwise go unnoticed until a datapack tried to write it, which is long after the point
     * where the list above is the thing that wants updating.
     */
    private static void warnAboutUnnamedColors() {
        var missing = IntStream.range(1, 64)
            .filter(id -> MapColor.byId(id).id == id && !names.containsKey(MapColor.byId(id)))
            .boxed()
            .toList();
        if (!missing.isEmpty()) Signpost.LOGGER.warn(
            "Minecraft knows map colors that Signpost has no name for and cannot write: ids {}. "
                + "Add them to MapColorSerializer.", missing);
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
