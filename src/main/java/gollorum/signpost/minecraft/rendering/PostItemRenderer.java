package gollorum.signpost.minecraft.rendering;

import com.mojang.blaze3d.matrix.MatrixStack;
import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.blockpartdata.types.renderers.BlockPartRenderer;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostItemRenderer extends ItemStackTileEntityRenderer {

    private static PostItemRenderer instance;
    public static PostItemRenderer getInstance() {
        if(instance == null) instance = new PostItemRenderer();
        return instance;
    }
    private PostItemRenderer() {}

    @Override
    public void renderByItem(ItemStack stack, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int combinedLight, int combinedOverlay) {
        if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof PostBlock)) {
            Signpost.LOGGER.error("Tried to render a non-post item with the post renderer");
            super.renderByItem(stack, matrixStack, renderTypeBuffer, combinedLight, combinedOverlay);
            return;
        }
        List<BlockPartInstance> parts;
        if(stack.hasTag() && stack.getTag().contains("Parts")) {
            parts = PostTile.readPartInstances(stack.getTag().getCompound("Parts"));
        } else {
            parts = new ArrayList<>();
            PostBlock.ModelType type = ((PostBlock)((BlockItem) stack.getItem()).getBlock()).type;
            parts.add(new BlockPartInstance(new PostBlockPart(type.postTexture), Vector3.ZERO));
            parts.add(new BlockPartInstance(new SmallWideSignBlockPart(
                Angle.fromDegrees(180), "", true, type.mainTexture, type.secondaryTexture, Optional.empty(),
                Colors.white, Optional.empty(), ItemStack.EMPTY, type, false
            ), new Vector3(0, 0.75f, 0)));
        }

        RenderingUtil.wrapInMatrixEntry(matrixStack, () -> {
            matrixStack.translate(0.5, 0, 0.5);

            for (BlockPartInstance now: parts) {
                RenderingUtil.wrapInMatrixEntry(matrixStack, () ->
                    BlockPartRenderer.renderGuiDynamic(
                        now.blockPart, matrixStack, now.offset, renderTypeBuffer, combinedLight, combinedOverlay
                    ));
            }

            if(renderTypeBuffer instanceof IRenderTypeBuffer.Impl) ((IRenderTypeBuffer.Impl) renderTypeBuffer).endBatch();
        });
    }
}
