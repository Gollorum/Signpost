package gollorum.signpost.minecraft.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElementType;

public class JigsawDeserializers {

	public static final StructurePoolElementType<SignpostJigsawPiece> signpost =
		register("signpost_pool_element", SignpostJigsawPiece.codec);

	public static final StructurePoolElementType<WaystoneJigsawPiece> waystone =
		register("signpost_waystone_pool_element", WaystoneJigsawPiece.codec);

	private static <P extends StructurePoolElement> StructurePoolElementType<P> register(String name, Codec<P> codec) {
		return Registry.register(Registry.STRUCTURE_POOL_ELEMENT, name, () -> codec);
	}
}
