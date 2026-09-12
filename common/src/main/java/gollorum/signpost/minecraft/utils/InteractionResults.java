package gollorum.signpost.minecraft.utils;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

/**
 * 1.21.1 splits block interaction in two: {@code useWithoutItem} returns an {@link InteractionResult}
 * while {@code useItemOn} returns an {@link ItemInteractionResult}. The blocks here share one
 * {@code use} method for both, so this maps its result onto the item-side enum.
 */
public final class InteractionResults {

    private InteractionResults() {}

    public static ItemInteractionResult forItem(InteractionResult result) {
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            // PASS falls through to useWithoutItem, which is what the caller expects.
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }
}
