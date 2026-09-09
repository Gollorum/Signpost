package gollorum.signpost.minecraft.storage;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.PlayerHandle;
import gollorum.signpost.Signpost;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.worldgen.VillageWaystone;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WaystoneLibraryStorage extends SavedData {

    public final HashMap<WaystoneHandle.Vanilla, WaystoneLibrary.WaystoneEntry> allWaystones;
    public final HashMap<PlayerHandle, HashSet<WaystoneHandle.Vanilla>> playerMemory;
    public final VillageWaystone villageWaystones;

    public WaystoneLibraryStorage(Map<WaystoneHandle.Vanilla, WaystoneLibrary.WaystoneEntry> allWaystones, Map<PlayerHandle, HashSet<WaystoneHandle.Vanilla>> playerMemory, VillageWaystone villageWaystones) {
        this.allWaystones = allWaystones instanceof HashMap<WaystoneHandle.Vanilla, WaystoneLibrary.WaystoneEntry> hm
            ? hm
            : new HashMap<>(allWaystones);
        this.playerMemory = playerMemory instanceof HashMap<PlayerHandle, HashSet<WaystoneHandle.Vanilla>> hm
            ? hm
            : new HashMap<>(playerMemory);
        this.villageWaystones = villageWaystones;
    }

    private WaystoneLibraryStorage() {
        this(new HashMap<>(), new HashMap<>(), new VillageWaystone());
    }

    private static final Codec<Map<WaystoneHandle.Vanilla, WaystoneLibrary.WaystoneEntry>> WAYSTONE_MAP_CODEC =
        Codec.mapPair(
            WaystoneHandle.Vanilla.CODEC.fieldOf("Waystone"),
            WaystoneLibrary.WaystoneEntry.CODEC
        ).codec().listOf().xmap(
            list -> list.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)),
            map -> map.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));

    private static final Codec<Map<PlayerHandle, HashSet<WaystoneHandle.Vanilla>>> PLAYER_MEMORY_CODEC =
        Codec.mapPair(
            PlayerHandle.DIRECT_CODEC.fieldOf("Player"),
            WaystoneHandle.Vanilla.CODEC.codec().listOf().fieldOf("DiscoveredWaystones")
        ).codec().listOf().xmap(
            list -> list.stream().collect(Collectors.toMap(Pair::getFirst, entry -> new HashSet<>(entry.getSecond()))),
            map -> map.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue().stream().toList()))
                .collect(Collectors.toList()));

    public static final Codec<WaystoneLibraryStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        WAYSTONE_MAP_CODEC.fieldOf("Waystones").forGetter(storage -> storage.allWaystones),
        PLAYER_MEMORY_CODEC.fieldOf("PlayerMemory").forGetter(storage -> storage.playerMemory),
        VillageWaystone.CODEC.fieldOf("villageWaystones").forGetter(storage -> storage.villageWaystones)
    ).apply(instance, WaystoneLibraryStorage::new));

    /**
     * The file this saved data used to live in, directly under the dimension's {@code data/} folder.
     *
     * <p>26.1 changed {@link SavedDataType}'s id from a plain string to an {@link Identifier}, and
     * {@code SavedDataStorage} resolves that as {@code data/<namespace>/<path>.dat} - the namespace
     * directory is not optional, so no Identifier can name the old flat file. The library therefore
     * has to be moved on disk once, or every pre-26.1 world would come up with no waystones at all.
     * {@link #migrateLegacyFile} does that; see WaystoneLibrary#initializeServer.
     */
    public static final String LEGACY_NAME = Signpost.MOD_ID + "_WaystoneLibrary";

    public static final Identifier ID =
        Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "waystone_library");

    public static final SavedDataType<WaystoneLibraryStorage> TYPE = new SavedDataType<>(
        ID,
        WaystoneLibraryStorage::new,
        WaystoneLibraryStorage.CODEC,
        DataFixTypes.SAVED_DATA_MAP_DATA
    );


    /**
     * Copies a pre-26.1 waystone library into the location 26.1 reads it from.
     *
     * <p>Copies rather than moves, so that the world can still be opened by an older version of the
     * mod. Only ever runs when the new file does not exist yet, so a library that has since been
     * written by 26.1 is never overwritten by a stale copy.
     */
    public static void migrateLegacyFile(ServerLevel overworld) {
        try {
            // Before 26.1 the overworld's saved data sat directly under <world>/data. 26.1 gave
            // every dimension its own folder - the overworld's is <world>/dimensions/minecraft/
            // overworld/data - and namespaced the file inside it. Both halves moved, so the source
            // and the destination have to be computed separately; deriving the old path from the
            // new folder finds nothing and skips the migration without a word.
            Path legacy = overworld.getServer().getWorldPath(LevelResource.DATA)
                .resolve(LEGACY_NAME + ".dat");
            Path current = DimensionType
                .getStorageFolder(overworld.dimension(), overworld.getServer().getWorldPath(LevelResource.ROOT))
                .resolve("data")
                .resolve(ID.getNamespace())
                .resolve(ID.getPath() + ".dat");
            if (!Files.isRegularFile(legacy) || Files.exists(current)) return;
            Files.createDirectories(current.getParent());
            Files.copy(legacy, current, StandardCopyOption.COPY_ATTRIBUTES);
            Signpost.LOGGER.info(
                "Migrated the waystone library from {} to {}; 26.1 resolves saved data through an Identifier.",
                legacy, current);
        } catch (IOException e) {
            Signpost.LOGGER.error("Failed to migrate the pre-26.1 waystone library. "
                + "Waystones saved by an earlier version will not be visible.", e);
        }
    }

}
