package gollorum.signpost.minecraft.worldgen;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.Config;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.stream.Collectors;

public class VillageWaystone {

    public static class ChunkEntryKey {
        public final ChunkPos chunkPos;
        public final ResourceLocation dimensionKey;

        public ChunkEntryKey(ChunkPos chunkPos, ResourceLocation dimensionKey) {
            this.chunkPos = chunkPos;
            this.dimensionKey = dimensionKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkEntryKey that = (ChunkEntryKey) o;
            return chunkPos.equals(that.chunkPos) && dimensionKey.equals(that.dimensionKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkPos, dimensionKey);
        }

        public static final Serializer serializer = new Serializer();
        public static class Serializer implements CompoundSerializable<ChunkEntryKey> {

            @Override
            public Class<ChunkEntryKey> getTargetClass() {
                return ChunkEntryKey.class;
            }

            @Override
            public CompoundTag write(ChunkEntryKey key, CompoundTag compound) {
                compound.putInt("x", key.chunkPos.x);
                compound.putInt("z", key.chunkPos.z);
                ResourceLocationSerializer.Instance.write(key.dimensionKey, compound);
                return compound;
            }

            @Override
            public boolean isContainedIn(CompoundTag compound) {
                return compound.contains("x") && compound.contains("z") &&
                    ResourceLocationSerializer.Instance.isContainedIn(compound);
            }

            @Override
            public ChunkEntryKey read(CompoundTag compound) {
                return new ChunkEntryKey(
                    new ChunkPos(compound.getInt("x"), compound.getInt("z")),
                    ResourceLocationSerializer.Instance.read(compound)
                );
            }
        }
    }

    // Key is not the position of the block, it's a reference position.
    // This is usually the village's position.
    private static final Map<BlockPos, WaystoneHandle.Vanilla> generatedWaystones = new HashMap<>();
    private static final Map<ChunkEntryKey, WaystoneHandle.Vanilla> generatedWaystonesByChunk = new HashMap<>();

    public static boolean doesWaystoneExistIn(BlockPos villageLocation) {
        return generatedWaystones.containsKey(villageLocation);
    }
    public static void register(String name, BlockPos referencePos, ServerLevel world, BlockPos blockPos) {
		WaystoneLibrary.getInstance().getHandleByName(name).ifPresent(handle -> {
			ChunkEntryKey key = new ChunkEntryKey(new ChunkPos(blockPos), world.dimension().location());
			generatedWaystones.put(referencePos, handle);
			generatedWaystonesByChunk.put(key, handle);
			WaystoneLibrary.getInstance().markDirty();
			WaystoneDiscoveryEventListener.registerNew(handle, world, blockPos);
		});
	}

    public static void reset() {
        generatedWaystones.clear();
        WaystoneDiscoveryEventListener.initialize();
    }

    public static Tag serialize() {
        ListTag ret = new ListTag();
        ret.addAll(generatedWaystones.entrySet().stream().map(
            e -> {
                CompoundTag compound = new CompoundTag();
                compound.put("refPos", BlockPosSerializer.INSTANCE.write(e.getKey()));
                generatedWaystonesByChunk.entrySet().stream().filter(ce -> ce.getValue().equals(e.getValue())).findFirst()
                    .ifPresent(ce -> compound.put("chunkEntryKey", ChunkEntryKey.serializer.write(ce.getKey())));
                compound.put("waystone", WaystoneHandle.Vanilla.Serializer.write(e.getValue()));
                return compound;
            }).toList());
        return ret;
    }

    public static void deserialize(ListTag nbt) {
        generatedWaystones.clear();
        generatedWaystones.putAll(
            nbt.stream().collect(Collectors.toMap(
                entry -> BlockPosSerializer.INSTANCE.read(((CompoundTag) entry).getCompound("refPos")),
                entry -> WaystoneHandle.Vanilla.Serializer.read(((CompoundTag) entry).getCompound("waystone"))
            )));
        generatedWaystonesByChunk.clear();
        generatedWaystonesByChunk.putAll(
            nbt.stream().collect(Collectors.toMap(
                entry -> ChunkEntryKey.serializer.read(((CompoundTag) entry).getCompound("chunkEntryKey")),
                entry -> WaystoneHandle.Vanilla.Serializer.read(((CompoundTag) entry).getCompound("waystone"))
            )));
    }


	public static Set<Map.Entry<BlockPos, WaystoneHandle.Vanilla>> getAllEntries(ResourceLocation dimension) {
		List<BlockPos> toRemove = generatedWaystones.entrySet().stream()
            .filter(e -> WaystoneLibrary.getInstance().getData(e.getValue()).isEmpty())
            .map(Map.Entry::getKey).toList();
		for(BlockPos key : toRemove) generatedWaystones.remove(key);
		return generatedWaystones.entrySet().stream()
            .filter(e -> dimensionOf(e.getValue()).map(d -> d.equals(dimension)).orElse(true))
            .collect(Collectors.toSet());
	}

    private static Optional<ResourceLocation> dimensionOf(WaystoneHandle.Vanilla handle) {
        return generatedWaystonesByChunk.entrySet().stream()
            .filter(e -> e.getValue().equals(handle))
            .findFirst()
            .map(e -> e.getKey().dimensionKey);
    }

	public static Map<ChunkEntryKey, WaystoneHandle.Vanilla> getAllEntriesByChunk() {
		List<ChunkEntryKey> toRemove = generatedWaystonesByChunk.entrySet().stream()
            .filter(e -> WaystoneLibrary.getInstance().getData(e.getValue()).isEmpty())
            .map(Map.Entry::getKey).toList();
		for(ChunkEntryKey key : toRemove) generatedWaystonesByChunk.remove(key);
		return generatedWaystonesByChunk;
	}

	private static List<ModelWaystone> getAllowedWaystones() {
		return ModelWaystone.variants.stream()
			.filter(v -> Config.Server.worldGen.allowedVillageWaystones().contains(v.name))
			.map(v -> v.getBlock())
			.collect(Collectors.toList());
	}


}
