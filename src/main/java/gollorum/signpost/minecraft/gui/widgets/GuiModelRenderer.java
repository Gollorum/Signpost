package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.Flippable;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.rendering.FlippableModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class GuiModelRenderer implements Widget, Flippable {

    private final FlippableModel model;
    private final float modelSpaceXOffset;
    private final float modelSpaceYOffset;
    private final ItemStack stack;
    private boolean isFlipped = false;

    private final Point center;
    private final int width;
    private final int height;

    public final Rect rect;

    public GuiModelRenderer(Rect rect, FlippableModel model, float modelSpaceXOffset, float modelSpaceYOffset, ItemStack stack) {
        this.rect = rect;
        center = rect.center();
        width = rect.width;
        height = rect.height;
        this.model = model;
        this.modelSpaceXOffset = modelSpaceXOffset;
        this.modelSpaceYOffset = modelSpaceYOffset;
        this.stack = stack;
    }


    public boolean isFlipped() {
        return isFlipped;
    }

    public void setFlipped(boolean isFlipped) {
        this.isFlipped = isFlipped;
    }

    public void render(@Nonnull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        matrixStack = new PoseStack();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.translate(center.x, center.y, 100);
        float scale = Math.min(width, height);
        matrixStack.scale(scale, -scale, scale);
        matrixStack.translate(modelSpaceXOffset, modelSpaceYOffset, 0);
        if(isFlipped) {
            matrixStack.translate(0, 0, -1f);
            matrixStack.scale(-1, 1, -1);
        }
        matrixStack.translate(0.5f, 0.5f, 0);
        MultiBufferSource.BufferSource renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
        Lighting.setupForFlatItems();

        Minecraft.getInstance().getItemRenderer().render(
            stack,
            ItemTransforms.TransformType.GUI,
            false,
            matrixStack,
            renderTypeBuffer,
            0xf000f0,
            OverlayTexture.NO_OVERLAY,
            model.get(isFlipped)
        );
        renderTypeBuffer.endBatch();
        RenderSystem.enableDepthTest();
    }

}
