package gollorum.signpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.compat.ExternalWaystoneLibrary;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface WaystoneHandle {

    public static final StreamCodec<ByteBuf, WaystoneHandle> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.dispatch(
        WaystoneHandle::typeTag,
        type -> {
            if (type.equals(Vanilla.typeTag)) return Vanilla.STREAM_CODEC;
            else return ExternalWaystoneLibrary.getInstance().getStreamCodec(type).orElseThrow();
        }
    );

//    public static final Codec<WaystoneHandle> CODEC = Codec.STRING.fieldOf("type").codec().partialDispatch(
//        "type",
//        handle -> DataResult.success(handle.typeTag()),
//        type -> {
//            if (type.equals(Vanilla.typeTag)) return DataResult.success(Vanilla.CODEC);
//            else return ExternalWaystoneLibrary.getInstance().getCodec(type)
//                .map(DataResult::success)
//                .orElseGet(() -> DataResult.error(() -> "Unknown waystone type: " + type));
//        }
//    );

    public static final MapCodec<WaystoneHandle> MAP_CODEC = Codec.STRING.dispatchMap("type",
        WaystoneHandle::typeTag,
        type -> {
            if (type.equals(Vanilla.typeTag)) return Vanilla.CODEC;
            else return ExternalWaystoneLibrary.getInstance().getCodec(type)
                .orElseGet(() -> MapCodec.unit(Vanilla.NIL));
        }
    );

    String typeTag();

    public static record Vanilla(UUID id) implements WaystoneHandle {
        public static final String typeTag = "vanilla";

        @Override
        public String typeTag() {
            return typeTag;
        }

        public static final Vanilla NIL = new Vanilla(Util.NIL_UUID);

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vanilla that = (Vanilla) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public static final MapCodec<Vanilla> CODEC = UUIDUtil.CODEC.fieldOf("Id").xmap(Vanilla::new, Vanilla::id);

        public static final StreamCodec<ByteBuf, Vanilla> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            Vanilla::id,
            Vanilla::new
        );

    }

}