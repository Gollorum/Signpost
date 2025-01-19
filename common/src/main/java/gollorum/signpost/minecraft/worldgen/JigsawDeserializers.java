package gollorum.signpost.minecraft.worldgen;

import com.mojang.serialization.Codec;
import gollorum.signpost.Signpost;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;

import java.util.function.Supplier;

public class JigsawDeserializers {

    public static void register() {
        var signpostType = register("signpost_pool_element", SignpostJigsawPiece.codec);
        var waystoneType = register("waystone_pool_element", WaystoneJigsawPiece.codec);

        signpost = () -> signpostType;
        waystone = () -> waystoneType;

        // incorrect domain
        registerLegacy("signpost_pool_element", SignpostJigsawPiece.codec);
        registerLegacy("signpost_waystone_pool_element", WaystoneJigsawPiece.codec);
    }

    public static Supplier<StructurePoolElementType<SignpostJigsawPiece>> signpost = null;
    public static Supplier<StructurePoolElementType<WaystoneJigsawPiece>> waystone = null;

    private static <P extends StructurePoolElement> StructurePoolElementType<P> register(String name, Codec<P> codec) {
        return Registry.register(BuiltInRegistries.STRUCTURE_POOL_ELEMENT, ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, name), () -> codec);
    }
    private static <P extends StructurePoolElement> StructurePoolElementType<P> registerLegacy(String name, Codec<P> codec) {
        return Registry.register(BuiltInRegistries.STRUCTURE_POOL_ELEMENT, name, () -> codec);
    }
}