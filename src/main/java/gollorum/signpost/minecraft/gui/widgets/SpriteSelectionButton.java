package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SpriteSelectionButton extends AbstractButton {

    private final TextureAtlasSprite sprite;
    private final Consumer<SpriteSelectionButton> onPressed;

    public SpriteSelectionButton(
        Rect rect, TextureAtlasSprite sprite,
        Consumer<SpriteSelectionButton> pressedAction
    ) {
        super(
            rect.point.x, rect.point.y,
            rect.width, rect.height,
            Component.literal("")
        );
        onPressed = pressedAction;
        if(sprite.getWidth() > sprite.getHeight())
            height = width * sprite.getHeight() / sprite.getWidth();
        else if (sprite.getWidth() < sprite.getHeight())
            width = height * sprite.getWidth() / sprite.getHeight();
        this.sprite = sprite;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();

        RenderSystem.enableTexture();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, sprite.atlas().location());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Matrix4f matrix = matrixStack.last().pose();
        float blitOffset = 0f;
        int xMin = x;
        int xMax = xMin + width;
        int yMin = y;
        int yMax = yMin + height;
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix, xMin, yMax, blitOffset).uv(sprite.getU0(), sprite.getV1()).endVertex();
        bufferbuilder.vertex(matrix, xMax, yMax, blitOffset).uv(sprite.getU1(), sprite.getV1()).endVertex();
        bufferbuilder.vertex(matrix, xMax, yMin, blitOffset).uv(sprite.getU1(), sprite.getV0()).endVertex();
        bufferbuilder.vertex(matrix, xMin, yMin, blitOffset).uv(sprite.getU0(), sprite.getV0()).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
        RenderSystem.disableBlend();
//        BufferUploader.end(bufferbuilder);
        if(isHovered) GuiComponent.fill(matrixStack, xMin, yMin, xMax, yMax, 0x50ffffff);

    }

    @Override
    public void onPress() {
        onPressed.accept(this);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }
}
