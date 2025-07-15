package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.math.geometry.Vector3;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * @param spawn Global.
 */
public record WaystoneLocationData(WorldLocation block, Vector3 spawn) {

    public WaystoneLocationData withoutExplicitLevel() {
        if (block.world().isLeft()) {
            return new WaystoneLocationData(new WorldLocation(block.blockPos(), Either.right(block.world().leftOrThrow().dimension().location())), spawn);
        } else {
            return this;
        }
    }

    public static final Codec<WaystoneLocationData> CODEC = RecordCodecBuilder.create(i -> i.group(
        WorldLocation.CODEC.fieldOf("Block").forGetter(WaystoneLocationData::block),
        Vector3.CODEC.fieldOf("Spawn").forGetter(WaystoneLocationData::spawn)
    ).apply(i, WaystoneLocationData::new));

    public static final StreamCodec<ByteBuf, WaystoneLocationData> STREAM_CODEC = StreamCodec.composite(
        WorldLocation.STREAM_CODEC, WaystoneLocationData::block,
        Vector3.STREAM_CODEC, WaystoneLocationData::spawn,
        WaystoneLocationData::new
    );
}
