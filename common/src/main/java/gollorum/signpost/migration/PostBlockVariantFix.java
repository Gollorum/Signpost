package gollorum.signpost.migration;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.References;

/**
 * This DataFixer injects the variant data into the block entity NBT, based on the old block ID.
 * You also need a separate block state renaming fixer (see comment below).
 */
public class PostBlockVariantFix extends DataFix {

    public PostBlockVariantFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    public TypeRewriteRule makeRule() {
        final OpticFinder<String> idFinder = DSL.fieldFinder("id", DSL.string());
        final OpticFinder<Dynamic<?>> nbtFinder = DSL.fieldFinder("nbt", DSL.remainderType());
        return fixTypeEverywhereTyped(
            "PostBlockVariantFix",
            getInputSchema().getType(References.BLOCK_ENTITY),
            typed -> {
                String id = typed.getOptional(idFinder).orElse("");
                if (id.startsWith("signpost:post_")) {
                    String variant = id.substring("signpost:post_".length());
                    Dynamic<?> nbt = typed.getOptional(nbtFinder).orElse(null);
                    if (nbt != null) {
                        return typed.update(nbtFinder, old -> old.set("variant", old.createString(variant)));
                    }
                }
                return typed;
            }
        );
    }

    // You also need a block state renaming fix, e.g.:
    // DataFix blockRenameFix = new BlockRenameFix(outputSchema, Map.of(
    //     "signpost:post_oak", "signpost:post",
    //     "signpost:post_birch", "signpost:post"
    //     // ...
    // ));
    // Register both fixes in your mod's DataFixer registration.
}
