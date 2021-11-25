package gollorum.signpost.minecraft.worldgen;

import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.jigsaw.IJigsawDeserializer;

public class JigsawDeserializers {

	public static final IJigsawDeserializer signpost =
		register("signpost_pool_element", SignpostJigsawPiece::new);

	public static final IJigsawDeserializer waystone =
		register("signpost_waystone_pool_element", WaystoneJigsawPiece::new);

	private static IJigsawDeserializer register(String name, IJigsawDeserializer serializer) {
		return Registry.register(Registry.STRUCTURE_POOL_ELEMENT, name, serializer);
	}
}
