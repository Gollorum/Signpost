package gollorum.signpost.minecraft.worldgen;

import com.mojang.serialization.Codec;
import gollorum.signpost.Signpost;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;

public class JigsawDeserializers {

	public static void register() {
		signpost = register("signpost_pool_element", SignpostJigsawPiece.codec);
		waystone = register("waystone_pool_element", WaystoneJigsawPiece.codec);

		// incorrect domain
		registerLegacy("signpost_pool_element", SignpostJigsawPiece.codec);
		registerLegacy("signpost_waystone_pool_element", WaystoneJigsawPiece.codec);
	}

	public static StructurePoolElementType<SignpostJigsawPiece> signpost = null;
	public static StructurePoolElementType<WaystoneJigsawPiece> waystone = null;

	private static <P extends StructurePoolElement> StructurePoolElementType<P> register(String name, Codec<P> codec) {
		return Registry.register(Registry.STRUCTURE_POOL_ELEMENT, new ResourceLocation(Signpost.MOD_ID, name), () -> codec);
	}
	private static <P extends StructurePoolElement> StructurePoolElementType<P> registerLegacy(String name, Codec<P> codec) {
		return Registry.register(Registry.STRUCTURE_POOL_ELEMENT, name, () -> codec);
	}
}
