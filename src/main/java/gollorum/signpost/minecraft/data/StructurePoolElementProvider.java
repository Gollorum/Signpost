package gollorum.signpost.minecraft.data;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import gollorum.signpost.Signpost;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public abstract class StructurePoolElementProvider implements DataProvider {

    private final PackOutput.PathProvider pathProvider;

    public StructurePoolElementProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "structures");
    }

    protected abstract void buildElements(BiConsumer<StructurePoolElement, ResourceLocation> registerElement);

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput output) {
        Set<ResourceLocation> set = Sets.newHashSet();
        List<CompletableFuture<?>> list = new ArrayList<>();

        buildElements((elem, id) -> {
            if (!set.add(id)) {
                throw new IllegalStateException("Duplicate recipe " + id);
            } else {
                Codec<StructurePoolElement> codec = (Codec<StructurePoolElement>) elem.getType().codec();
                DataResult<JsonElement> serializationResult = codec.encodeStart(JsonOps.INSTANCE, elem);
                var option = serializationResult.resultOrPartial(str -> Signpost.LOGGER.error("Failed to serialize structure pool element: " + str));
                option.ifPresent(t -> list.add(DataProvider.saveStable(output, t, this.pathProvider.json(id))));
            }
        });

        return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
    }
}
