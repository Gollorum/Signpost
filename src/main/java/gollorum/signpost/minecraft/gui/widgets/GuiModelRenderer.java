package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import gollorum.signpost.minecraft.gui.utils.Flippable;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.rendering.FlippableModel;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class GuiModelRenderer implements IRenderable, Flippable {

    private final FlippableModel model;
    private final float modelSpaceXOffset;
    private final float modelSpaceYOffset;
    private final ItemStack stack;
    private boolean isFlipped = false;

    private final Point center;
    private final int width;
    private final int height;

    public final Rect rect;
    private final RenderType renderType;

    public GuiModelRenderer(Rect rect, FlippableModel model, float modelSpaceXOffset, float modelSpaceYOffset, ItemStack stack, RenderType renderType) {
        this.rect = rect;
        center = rect.center();
        width = rect.width;
        height = rect.height;
        this.model = model;
        this.modelSpaceXOffset = modelSpaceXOffset;
        this.modelSpaceYOffset = modelSpaceYOffset;
        this.stack = stack;
        this.renderType = renderType;
    }


    public boolean isFlipped() {
        return isFlipped;
    }

    public void setFlipped(boolean isFlipped) {
        this.isFlipped = isFlipped;
    }

    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        float scale = Math.min(width, height);
        RenderingUtil.renderGui(
            model.get(isFlipped),
            center,
            Angle.ZERO,
            Angle.ZERO,
            scale,
            new Vector3(modelSpaceXOffset, modelSpaceYOffset, 0),
            isFlipped,
            renderType
        );
    }

}
