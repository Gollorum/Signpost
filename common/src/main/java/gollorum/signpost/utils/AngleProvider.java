package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.serialization.BufferSerializable;
import gollorum.signpost.utils.serialization.CompoundSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.animal.Cod;

import java.util.Optional;

public interface AngleProvider {

    Angle get();

    public static final record Literal(Angle angle) implements AngleProvider {
        public static final MapCodec<Literal> Codec = RecordCodecBuilder.mapCodec(i -> i.group(
            Angle.Codec.fieldOf("angle").forGetter(Literal::angle)
        ).apply(i, Literal::new));

        @Override
        public Angle get() { return angle; }
    }

    public static final class WaystoneTarget implements AngleProvider {
        public static final MapCodec<WaystoneTarget> Codec = RecordCodecBuilder.mapCodec(i -> i.group(
            Angle.Codec.fieldOf("cachedAngle").forGetter(WaystoneTarget::get)
        ).apply(i, WaystoneTarget::new));

        private Angle cachedAngle;
        public void setCachedAngle(Angle cachedAngle) { this.cachedAngle = cachedAngle; }

        public WaystoneTarget(Angle cachedAngle) {
            this.cachedAngle = cachedAngle;
        }

        @Override
        public Angle get() { return cachedAngle; }
    }

    public static final Codec<AngleProvider> Codec = RecordCodecBuilder.create(i -> i.group(
        com.mojang.serialization.Codec.STRING.fieldOf("type").forGetter(a -> {
            if (a instanceof Literal) return "literal";
            else if (a instanceof WaystoneTarget) return "waystone";
            else throw new RuntimeException("Invalid angle provider type " + a.getClass());
        }),
        Angle.Codec.optionalFieldOf("angle").forGetter(a -> a instanceof Literal(Angle angle) ? Optional.of(angle) : Optional.empty()),
        Angle.Codec.optionalFieldOf("cachedAngle").forGetter(a -> a instanceof WaystoneTarget w ? Optional.of(w.get()) : Optional.empty()),
        com.mojang.serialization.Codec.FLOAT.optionalFieldOf("Radians").forGetter(a -> Optional.empty())
    ).apply(i, (type, literal, waystone, legacyLiteral) ->
        switch (type) {
            case "literal" -> new Literal(literal.get());
            case "waystone" -> new WaystoneTarget(waystone.get());
            default -> new Literal(Angle.fromRadians(legacyLiteral.get()));
        }
    ));

}
