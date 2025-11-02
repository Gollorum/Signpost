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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

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

    public static final String NAME = Signpost.MOD_ID + "_WaystoneLibrary";

    public static final SavedDataType<WaystoneLibraryStorage> TYPE = new SavedDataType<>(
        NAME,
        WaystoneLibraryStorage::new,
        WaystoneLibraryStorage.CODEC,
        DataFixTypes.SAVED_DATA_MAP_DATA
    );

}
