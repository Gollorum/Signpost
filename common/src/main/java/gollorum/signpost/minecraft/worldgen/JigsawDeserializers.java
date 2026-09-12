package gollorum.signpost.minecraft.worldgen;

import gollorum.signpost.Signpost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;

import java.util.function.BiConsumer;

public class JigsawDeserializers {

    public static void register(BiConsumer<ResourceLocation, StructurePoolElementType<?>> register) {
        register.accept(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "signpost_pool_element"), signpost);
        register.accept(ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "waystone_pool_element"), waystone);
    }

    public static StructurePoolElementType<SignpostJigsawPiece> signpost = () -> SignpostJigsawPiece.codec;
    public static StructurePoolElementType<WaystoneJigsawPiece> waystone = () -> WaystoneJigsawPiece.codec;
}