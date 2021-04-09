package gollorum.signpost.minecraft.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.jigsaw.IJigsawDeserializer;
import net.minecraft.world.gen.feature.jigsaw.JigsawPiece;

public class JigsawDeserializers {

	public static final IJigsawDeserializer<SignpostJigsawPiece> signpost =
		register("signpost_pool_element", SignpostJigsawPiece.codec);

	public static final IJigsawDeserializer<WaystoneJigsawPiece> waystone =
		register("signpost_waystone_pool_element", WaystoneJigsawPiece.codec);

	private static <P extends JigsawPiece> IJigsawDeserializer<P> register(String name, Codec<P> codec) {
		return Registry.register(Registry.STRUCTURE_POOL_ELEMENT, name, () -> codec);
	}
}
