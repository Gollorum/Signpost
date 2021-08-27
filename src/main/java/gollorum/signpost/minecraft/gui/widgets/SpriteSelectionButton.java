package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.StringTextComponent;

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
            new StringTextComponent("")
        );
        onPressed = pressedAction;
        if(sprite.getWidth() > sprite.getHeight())
            height = width * sprite.getHeight() / sprite.getWidth();
        else if (sprite.getWidth() < sprite.getHeight())
            width = height * sprite.getWidth() / sprite.getHeight();
        this.sprite = sprite;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);

        Minecraft.getInstance().getTextureManager().bindTexture(sprite.getAtlasTexture().getTextureLocation());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Matrix4f matrix = matrixStack.getLast().getMatrix();
        float blitOffset = 0f;
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        int xMin = x;
        int xMax = xMin + width;
        int yMin = y;
        int yMax = yMin + height;
        bufferbuilder.pos(matrix, xMin, yMax, blitOffset).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        bufferbuilder.pos(matrix, xMax, yMax, blitOffset).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
        bufferbuilder.pos(matrix, xMax, yMin, blitOffset).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        bufferbuilder.pos(matrix, xMin, yMin, blitOffset).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        bufferbuilder.finishDrawing();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.draw(bufferbuilder);
        if(isHovered()) AbstractGui.fill(matrixStack, xMin, yMin, xMax, yMax, 0x50ffffff);

    }

    @Override
    public void onPress() {
        onPressed.accept(this);
    }
}
