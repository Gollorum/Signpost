package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.Either;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SpriteSelectionButton extends AbstractButton {

    private final Either<TextureAtlasSprite, Material> material;
    private final int tint;
    private final Consumer<SpriteSelectionButton> onPressed;

    public SpriteSelectionButton(
        Rect rect,
        Either<TextureAtlasSprite, Material> material,
        int tint,
        Consumer<SpriteSelectionButton> pressedAction
    ) {
        super(
            rect.point.x, rect.point.y,
            rect.width, rect.height,
            Component.literal("")
        );
        onPressed = pressedAction;
        this.material = material;
        if ((tint & 0xff000000) == 0)
            tint |= 0xff000000;
        this.tint = tint;
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var sprite = material.leftOr(graphics::getSprite);
        var contents = sprite.contents();
        if(contents.width() > contents.height())
            height = width * contents.height() / contents.width();
        else if (contents.width() < contents.height())
            width = height * contents.width() / contents.height();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height, tint);
        int xMin = this.getX();
        int xMax = xMin + width;
        int yMin = this.getY();
        int yMax = yMin + height;
        if(isHovered) graphics.fill(RenderPipelines.GUI, xMin, yMin, xMax, yMax, 0x50ffffff);

    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        onPressed.accept(this);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {

    }
}
