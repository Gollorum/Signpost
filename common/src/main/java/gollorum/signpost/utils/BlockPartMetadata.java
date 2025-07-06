package gollorum.signpost.utils;

import com.mojang.serialization.Codec;

public record BlockPartMetadata<T extends BlockPart>(String identifier, Codec<T> codec, Class<T> targetClass) { }
