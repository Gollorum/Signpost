package gollorum.signpost.registry;

import gollorum.signpost.minecraft.worldgen.JigsawDeserializers;
import gollorum.signpost.minecraft.worldgen.SignpostJigsawPiece;
import gollorum.signpost.minecraft.worldgen.WaystoneJigsawPiece;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static gollorum.signpost.Signpost.MOD_ID;

public class JigsawDeserializerRegistry {

    private static final DeferredRegister<StructurePoolElementType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.STRUCTURE_POOL_ELEMENT, MOD_ID);

    private static final DeferredHolder<StructurePoolElementType<?>, StructurePoolElementType<SignpostJigsawPiece>> POST =
        REGISTER.register("signpost_pool_element", () -> () -> SignpostJigsawPiece.codec);

    private static final DeferredHolder<StructurePoolElementType<?>, StructurePoolElementType<WaystoneJigsawPiece>> WAYSTONE =
        REGISTER.register("waystone_pool_element", () -> () -> WaystoneJigsawPiece.codec);

    public static void register(IEventBus bus){
        REGISTER.register(bus);

        JigsawDeserializers.signpost = POST;
        JigsawDeserializers.waystone = WAYSTONE;
    }
}
