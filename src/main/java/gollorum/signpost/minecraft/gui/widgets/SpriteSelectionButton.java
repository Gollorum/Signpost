package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.TextComponent;

import java.util.function.Consumer;

public class SpriteSelectionButton extends AbstractButton {

    private final TextureAtlasSprite sprite;
    private final float r;
    private final float g;
    private final float b;
    private final Consumer<SpriteSelectionButton> onPressed;

    public SpriteSelectionButton(
        Rect rect, TextureAtlasSprite sprite, int tint,
        Consumer<SpriteSelectionButton> pressedAction
    ) {
        super(
            rect.point.x, rect.point.y,
            rect.width, rect.height,
            new TextComponent("")
        );
        onPressed = pressedAction;
        if(sprite.getWidth() > sprite.getHeight())
            height = width * sprite.getHeight() / sprite.getWidth();
        else if (sprite.getWidth() < sprite.getHeight())
            width = height * sprite.getWidth() / sprite.getHeight();
        this.sprite = sprite;
        r = Colors.getRed(tint) / 255f;
        g = Colors.getGreen(tint) / 255f;
        b = Colors.getBlue(tint) / 255f;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(r, g, b, this.alpha);

        RenderSystem.setShaderTexture(0, sprite.atlas().location());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Matrix4f matrix = matrixStack.last().pose();
        float blitOffset = 0f;
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        int xMin = x;
        int xMax = xMin + width;
        int yMin = y;
        int yMax = yMin + height;
        bufferbuilder.vertex(matrix, xMin, yMax, blitOffset).uv(sprite.getU0(), sprite.getV1()).endVertex();
        bufferbuilder.vertex(matrix, xMax, yMax, blitOffset).uv(sprite.getU1(), sprite.getV1()).endVertex();
        bufferbuilder.vertex(matrix, xMax, yMin, blitOffset).uv(sprite.getU1(), sprite.getV0()).endVertex();
        bufferbuilder.vertex(matrix, xMin, yMin, blitOffset).uv(sprite.getU0(), sprite.getV0()).endVertex();
        bufferbuilder.end();
        BufferUploader.end(bufferbuilder);
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
