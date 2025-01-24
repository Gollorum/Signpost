package gollorum.signpost.minecraft.worldgen;

import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.ModelWaystone;
import gollorum.signpost.minecraft.config.IConfig;
import gollorum.signpost.platform.Services;
import gollorum.signpost.utils.serialization.BlockPosSerializer;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import gollorum.signpost.utils.serialization.ResourceLocationSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
            public void encode(CompoundTag compound, ChunkEntryKey key, HolderLookup.Provider provider) {
                compound.putInt("x", key.chunkPos.x);
                compound.putInt("z", key.chunkPos.z);
                ResourceLocationSerializer.COMPOUND.encode(compound, key.dimensionKey, provider);
            }

            @Override
            public boolean isContainedIn(CompoundTag compound) {
                return compound.contains("x") && compound.contains("z") &&
                    ResourceLocationSerializer.COMPOUND.isContainedIn(compound);
            }

            @Override
            public ChunkEntryKey decode(CompoundTag compound, HolderLookup.Provider provider) {
                return new ChunkEntryKey(
                    new ChunkPos(compound.getInt("x"), compound.getInt("z")),
                    ResourceLocationSerializer.COMPOUND.decode(compound, provider)
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
            Services.WAYSTONE_DISCOVERY_EVENT_LISTENER.registerNew(handle, world, blockPos);
		});
	}

    public static void reset() {
        generatedWaystones.clear();
        Services.WAYSTONE_DISCOVERY_EVENT_LISTENER.initialize();
    }

    public static Tag serialize(HolderLookup.Provider registryAccess) {
        ListTag ret = new ListTag();
        ret.addAll(generatedWaystones.entrySet().stream().map(
            e -> {
                CompoundTag compound = new CompoundTag();
                compound.put("refPos", BlockPosSerializer.COMPOUND.encode(e.getKey(), registryAccess));
                generatedWaystonesByChunk.entrySet().stream().filter(ce -> ce.getValue().equals(e.getValue())).findFirst()
                    .ifPresent(ce -> compound.put("chunkEntryKey", ChunkEntryKey.serializer.encode(ce.getKey(), registryAccess)));
                compound.put("waystone", WaystoneHandle.Vanilla.CompoundSerializer.encode(e.getValue(), registryAccess));
                return compound;
            }).toList());
        return ret;
    }

    public static void deserialize(ListTag nbt, HolderLookup.Provider registryAccess) {
        generatedWaystones.clear();
        generatedWaystones.putAll(
            nbt.stream().collect(Collectors.toMap(
                entry -> BlockPosSerializer.COMPOUND.decode(((CompoundTag) entry).getCompound("refPos"), registryAccess),
                entry -> WaystoneHandle.Vanilla.CompoundSerializer.decode(((CompoundTag) entry).getCompound("waystone"), registryAccess)
            )));
        generatedWaystonesByChunk.clear();
        generatedWaystonesByChunk.putAll(
            nbt.stream().collect(Collectors.toMap(
                entry -> ChunkEntryKey.serializer.decode(((CompoundTag) entry).getCompound("chunkEntryKey"), registryAccess),
                entry -> WaystoneHandle.Vanilla.CompoundSerializer.decode(((CompoundTag) entry).getCompound("waystone"), registryAccess)
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

	public static Map<ChunkEntryKey, WaystoneHandle.Vanilla> getAllEntriesByChunk(boolean validateExistence) {
        if(validateExistence) {
            List<ChunkEntryKey> toRemove = generatedWaystonesByChunk.entrySet().stream()
                .filter(e -> WaystoneLibrary.getInstance().getData(e.getValue()).isEmpty())
                .map(Map.Entry::getKey).toList();
            for (ChunkEntryKey key : toRemove) generatedWaystonesByChunk.remove(key);
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
