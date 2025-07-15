package gollorum.signpost.utils;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public interface NameProvider {

    String get();

    public record Literal(String name) implements NameProvider {

            @Override
            public String get() {
                return name;
            }

    }

    public static final class WaystoneTarget implements NameProvider {

        private String cachedName;
        public void setCachedName(String name) { cachedName = name; }

        @Override
        public String get() { return cachedName; }

        public WaystoneTarget(String cachedName) { this.cachedName = cachedName; }
    }

    private static NameProvider from(String type, String name) {
        return switch (type) {
            case "literal" -> new Literal(name);
            case "waystone" -> new WaystoneTarget(name);
            default -> throw new RuntimeException("Invalid name provider type " + type);
        };
    }

    public static final Codec<NameProvider> CODEC = Codec.STRING.dispatch("type",
        nameProvider -> {
            if (nameProvider instanceof Literal) {
                return "literal";
            } else if (nameProvider instanceof WaystoneTarget) {
                return "waystone";
            } else {
                throw new RuntimeException("Unknown NameProvider type: " + nameProvider.getClass());
            }
        },
        type ->
            switch (type) {
                case "literal" -> Codec.STRING.fieldOf("name").xmap(Literal::new, Literal::get);
                case "waystone" -> Codec.STRING.fieldOf("name").xmap(WaystoneTarget::new, WaystoneTarget::get);
                default -> throw new RuntimeException("Unknown NameProvider type: " + type);
            }
        );

    public static final StreamCodec<ByteBuf, NameProvider> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, t -> {
            if (t instanceof Literal) {
                return "literal";
            } else if (t instanceof WaystoneTarget) {
                return "waystone";
            } else {
                throw new RuntimeException("Unknown NameProvider type: " + t.getClass());
            }
        },
        ByteBufCodecs.STRING_UTF8, NameProvider::get,
        NameProvider::from
    );

}
