package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
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

        @Override
        public Angle get() { return angle; }

        @Override
        public String getTypeTag() { return "literal"; }
    }

    public static final class WaystoneTarget implements AngleProvider {
        public static final StreamCodec<ByteBuf, WaystoneTarget> STREAM_CODEC = Angle.STREAM_CODEC
            .map(WaystoneTarget::new, WaystoneTarget::get);

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

    public static final Codec<AngleProvider> CODEC = RecordCodecBuilder.create(i -> i.group(
        com.mojang.serialization.Codec.STRING.fieldOf("type").forGetter(AngleProvider::getTypeTag),
        Angle.CODEC.optionalFieldOf("angle").forGetter(a -> a instanceof Literal(Angle angle) ? Optional.of(angle) : Optional.empty()),
        Angle.CODEC.optionalFieldOf("cachedAngle").forGetter(a -> a instanceof WaystoneTarget w ? Optional.of(w.get()) : Optional.empty()),
        com.mojang.serialization.Codec.FLOAT.optionalFieldOf("Radians").forGetter(a -> Optional.empty())
    ).apply(i, (type, literal, waystone, legacyLiteral) ->
        switch (type) {
            case "literal" -> new Literal(literal.get());
            case "waystone" -> new WaystoneTarget(waystone.get());
            default -> new Literal(Angle.fromRadians(legacyLiteral.get()));
        }
    ));

    public static final StreamCodec<ByteBuf, AngleProvider> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.dispatch(AngleProvider::getTypeTag,
            type -> switch (type) {
                case "literal" -> Literal.STREAM_CODEC;
                case "waystone" -> WaystoneTarget.STREAM_CODEC;
                default -> Angle.STREAM_CODEC.map(Literal::new, Literal::angle);
            }
        );

}
