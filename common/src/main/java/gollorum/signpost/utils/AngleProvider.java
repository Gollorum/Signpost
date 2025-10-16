package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.math.Angle;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public interface AngleProvider {

    Angle get();

    String getTypeTag();

    public static final record Literal(Angle angle) implements AngleProvider {
        public static final StreamCodec<ByteBuf, Literal> STREAM_CODEC = Angle.STREAM_CODEC
            .map(Literal::new, Literal::angle);

        public static final MapCodec<AngleProvider> MAP_CODEC =
            Angle.CODEC.fieldOf("angle").xmap(
                Literal::new,
                AngleProvider::get
            );

        @Override
        public Angle get() { return angle; }

        @Override
        public String getTypeTag() { return "literal"; }
    }

    public static final class WaystoneTarget implements AngleProvider {
        public static final StreamCodec<ByteBuf, WaystoneTarget> STREAM_CODEC = Angle.STREAM_CODEC
            .map(WaystoneTarget::new, WaystoneTarget::get);

        public static final MapCodec<AngleProvider> MAP_CODEC =
            Angle.CODEC.fieldOf("cachedAngle").xmap(
                WaystoneTarget::new,
                AngleProvider::get
            );

        private Angle cachedAngle;
        public void setCachedAngle(Angle cachedAngle) { this.cachedAngle = cachedAngle; }

        public WaystoneTarget(Angle cachedAngle) {
            this.cachedAngle = cachedAngle;
        }

        @Override
        public Angle get() { return cachedAngle; }

        @Override
        public String getTypeTag() { return "waystone"; }
    }

    public static final MapCodec<AngleProvider> MAP_CODEC = Codec.STRING.dispatchMap(
        "type", AngleProvider::getTypeTag,
        type -> switch (type) {
            case "literal" -> Literal.MAP_CODEC;
            case "waystone" -> WaystoneTarget.MAP_CODEC;
            default -> Angle.MAP_CODEC.xmap(Literal::new, AngleProvider::get);
        }
    );

    public static final StreamCodec<ByteBuf, AngleProvider> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.dispatch(AngleProvider::getTypeTag,
            type -> switch (type) {
                case "literal" -> Literal.STREAM_CODEC;
                case "waystone" -> WaystoneTarget.STREAM_CODEC;
                default -> Angle.STREAM_CODEC.map(Literal::new, Literal::angle);
            }
        );

}
