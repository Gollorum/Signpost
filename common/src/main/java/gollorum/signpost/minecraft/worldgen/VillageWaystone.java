package gollorum.signpost.minecraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.platform.Services;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.stream.Collectors;

public class VillageWaystone {

    public static final Codec<VillageWaystone> CODEC = Entry.CODEC.listOf()
        .xmap(VillageWaystone::new, villageWaystone -> villageWaystone.allEntries);

    public record ChunkEntryKey(ChunkPos chunkPos, ResourceLocation dimensionKey) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkEntryKey that = (ChunkEntryKey) o;
            return chunkPos.equals(that.chunkPos) && dimensionKey.equals(that.dimensionKey);
        }

        public static final Codec<ChunkEntryKey> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("x").forGetter(c -> c.chunkPos.x),
            Codec.INT.fieldOf("z").forGetter(c -> c.chunkPos.z),
            ResourceLocation.CODEC.fieldOf("ResourceLocation").forGetter(ChunkEntryKey::dimensionKey)
        ).apply(i, (x, z, recloc) -> new ChunkEntryKey(new ChunkPos(x, z), recloc)));

    }

    // Key is not the position of the block, it's a reference position.
    // This is usually the village's position.
    private final Map<BlockPos, WaystoneHandle.Vanilla> generatedWaystones;
    private final Map<ChunkEntryKey, WaystoneHandle.Vanilla> generatedWaystonesByChunk;
    private final List<Entry> allEntries;

    public static VillageWaystone getInstance() {
        return WaystoneLibrary.getInstance().data.villageWaystones;
    }

    public VillageWaystone() {
        this.generatedWaystones = new HashMap<>();
        this.generatedWaystonesByChunk = new HashMap<>();
        this.allEntries = new ArrayList<>();
    }

    private VillageWaystone(List<Entry> generatedWaystones) {
        this.generatedWaystones = new HashMap<>();
        this.generatedWaystonesByChunk = new HashMap<>();
        this.allEntries = new ArrayList<>(generatedWaystones);
        for (Entry entry : generatedWaystones) {
            this.generatedWaystones.put(entry.referencePos, entry.handle);
            this.generatedWaystonesByChunk.put(entry.chunkEntryKey, entry.handle);
        }
    }

    private record Entry(WaystoneHandle.Vanilla handle, BlockPos referencePos, ChunkEntryKey chunkEntryKey) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            WaystoneHandle.Vanilla.CODEC.fieldOf("waystone").forGetter(Entry::handle),
            BlockPosSerializer.CODEC.fieldOf("refPos").forGetter(Entry::referencePos),
            ChunkEntryKey.CODEC.fieldOf("chunkEntryKey").forGetter(Entry::chunkEntryKey)
        ).apply(i, Entry::new));
    }

    public boolean doesWaystoneExistIn(BlockPos villageLocation) {
        return generatedWaystones.containsKey(villageLocation);
    }
    public void register(WaystoneLibrary waystoneLibrary, String name, BlockPos referencePos, ServerLevel world, BlockPos blockPos) {
        waystoneLibrary.getHandleByName(name).ifPresent(handle -> {
			ChunkEntryKey key = new ChunkEntryKey(new ChunkPos(blockPos), world.dimension().location());
			generatedWaystones.put(referencePos, handle);
			generatedWaystonesByChunk.put(key, handle);
            allEntries.add(new Entry(handle, referencePos, key));
            waystoneLibrary.markDirty();
            Services.WAYSTONE_DISCOVERY_EVENT_LISTENER.registerNew(handle, world, blockPos);
		});
	}

    public static void reset() {
        Services.WAYSTONE_DISCOVERY_EVENT_LISTENER.initialize();
    }

	public Set<Map.Entry<BlockPos, WaystoneHandle.Vanilla>> getAllEntries(WaystoneLibrary waystoneLibrary, ResourceLocation dimension) {
		var toRemove = allEntries.stream()
            .filter(e -> waystoneLibrary.getData(e.handle).isEmpty())
            .toList();
		for(Entry entry : toRemove) {
            generatedWaystones.remove(entry.referencePos);
            generatedWaystonesByChunk.remove(entry.chunkEntryKey);
            allEntries.remove(entry);
        }
        if (!toRemove.isEmpty())
            waystoneLibrary.markDirty();
		return generatedWaystones.entrySet().stream()
            .filter(e -> dimensionOf(e.getValue()).map(d -> d.equals(dimension)).orElse(true))
            .collect(Collectors.toSet());
	}

    private Optional<ResourceLocation> dimensionOf(WaystoneHandle.Vanilla handle) {
        return generatedWaystonesByChunk.entrySet().stream()
            .filter(e -> e.getValue().equals(handle))
            .findFirst()
            .map(e -> e.getKey().dimensionKey);
    }

	public Map<ChunkEntryKey, WaystoneHandle.Vanilla> getAllEntriesByChunk(WaystoneLibrary waystoneLibrary, boolean validateExistence) {
        if(validateExistence) {
            var toRemove = allEntries.stream()
                .filter(e -> waystoneLibrary.getData(e.handle).isEmpty())
                .toList();
            for (Entry entry : toRemove) {
                generatedWaystones.remove(entry.referencePos);
                generatedWaystonesByChunk.remove(entry.chunkEntryKey);
                allEntries.remove(entry);
            }
            if (!toRemove.isEmpty())
                waystoneLibrary.markDirty();
        }
		return generatedWaystonesByChunk;
	}

	private static List<ModelWaystone> getAllowedWaystones() {
		return ModelWaystone.variants.stream()
			.filter(v -> IConfig.IServer.getInstance().worldGen().allowedVillageWaystones().contains(v.name))
			.map(ModelWaystone.Variant::getBlock)
			.collect(Collectors.toList());
	}


}
