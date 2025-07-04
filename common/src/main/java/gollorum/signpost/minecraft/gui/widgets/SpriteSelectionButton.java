package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

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
            Component.literal("")
        );
        onPressed = pressedAction;
        if(sprite.contents().width() > sprite.contents().height())
            height = width * sprite.contents().height() / sprite.contents().width();
        else if (sprite.contents().width() < sprite.contents().height())
            width = height * sprite.contents().width() / sprite.contents().height();
        this.sprite = sprite;
        r = Colors.getRed(tint) / 255f;
        g = Colors.getGreen(tint) / 255f;
        b = Colors.getBlue(tint) / 255f;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blitSprite(RenderType::guiTextured, sprite, this.getX(), this.getY(), this.width, this.height);
        int xMin = this.getX();
        int xMax = xMin + width;
        int yMin = this.getY();
        int yMax = yMin + height;
        if(isHovered) graphics.fill(RenderType.guiOverlay(), xMin, yMin, xMax, yMax, 0x50ffffff);

    }

    @Override
    public void onPress() {
        onPressed.accept(this);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {

    }
}
