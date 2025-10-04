package gollorum.signpost.platform;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.tiles.IBlockEntityTypeFactory;
import gollorum.signpost.minecraft.gui.utils.IFluidTextureProvider;
import gollorum.signpost.minecraft.loot.ILootItemConditionRegistry;
import gollorum.signpost.minecraft.worldgen.BiomeAccessor;
import gollorum.signpost.minecraft.worldgen.IWaystoneDiscoveryEventListener;
import gollorum.signpost.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static final IFluidTextureProvider FLUID_TEXTURE_PROVIDER = load(IFluidTextureProvider.class);

    public static final IBlockEntityTypeFactory BLOCK_ENTITY_TYPE_FACTORY = load(IBlockEntityTypeFactory.class);

    public static final ILootItemConditionRegistry LOOT_ITEM_CONDITION_REGISTRY = load(ILootItemConditionRegistry.class);

    public static final IWaystoneDiscoveryEventListener WAYSTONE_DISCOVERY_EVENT_LISTENER = load(IWaystoneDiscoveryEventListener.class);

    public static final BiomeAccessor BIOME_ACCESSOR = load(BiomeAccessor.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Signpost.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}