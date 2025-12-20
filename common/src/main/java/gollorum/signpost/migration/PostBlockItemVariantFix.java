package gollorum.signpost.migration;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.References;

/**
 * This DataFixer injects the variant data into the block item NBT, derived from the old item ID.
 */
public class PostBlockItemVariantFix extends DataFix {

    public PostBlockItemVariantFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    public TypeRewriteRule makeRule() {
        final OpticFinder<String> idFinder = DSL.fieldFinder("id", DSL.string());
        final OpticFinder<Dynamic<?>> tagFinder = DSL.fieldFinder("tag", DSL.remainderType());
        return fixTypeEverywhereTyped(
            "PostBlockItemVariantFix",
            getInputSchema().getType(References.ITEM_STACK),
            typed -> {
                String id = typed.getOptional(idFinder).orElse("");
                if (id.startsWith("signpost:post_")) {
                    String variant = id.substring("signpost:post_".length());
                    // Change item id to unified block
                    typed = typed.set(idFinder, "signpost:post");
                    Dynamic<?> tag = typed.getOptional(tagFinder).orElse(null);
                    if (tag != null) {
                        typed = typed.update(tagFinder, old -> old.set("variant", old.createString(variant)));
                    } else {
                        // If no tag, create one using NbtOps.INSTANCE
                        Dynamic<?> newTag = new Dynamic<>(NbtOps.INSTANCE, NbtOps.INSTANCE.emptyMap());
                        newTag = newTag.set("variant", newTag.createString(variant));
                        typed = typed.set(tagFinder, newTag);
                    }
                }
                return typed;
            }
        );
    }
}
