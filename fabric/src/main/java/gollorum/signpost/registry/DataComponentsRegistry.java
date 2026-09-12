package gollorum.signpost.registry;

import gollorum.signpost.minecraft.data.PostData;
import gollorum.signpost.minecraft.data.WaystoneHandleData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import static gollorum.signpost.Signpost.MOD_ID;

public class DataComponentsRegistry {

    public static void register(){
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "post_data"), PostData.TYPE);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "waystone_data"), WaystoneHandleData.TYPE);
    }
}
