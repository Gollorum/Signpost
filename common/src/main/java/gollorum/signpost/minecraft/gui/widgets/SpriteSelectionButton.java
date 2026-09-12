package gollorum.signpost.minecraft.gui.widgets;

import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.utils.Either;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var sprite = material.leftOr(Material::sprite);
        var contents = sprite.contents();
        if(contents.width() > contents.height())
            height = width * contents.height() / contents.width();
        else if (contents.width() < contents.height())
            width = height * contents.width() / contents.height();
        graphics.blit(this.getX(), this.getY(), 0, this.width, this.height, sprite, 
            Colors.getRed(tint) / 255f, Colors.getGreen(tint) / 255f, Colors.getBlue(tint) / 255f, Colors.getAlpha(tint) / 255f);
        int xMin = this.getX();
        int xMax = xMin + width;
        int yMin = this.getY();
        int yMax = yMin + height;
        if(isHovered) graphics.fill(xMin, yMin, xMax, yMax, 0x50ffffff);

    }

    @Override
    public void onPress() {
        onPressed.accept(this);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_169152_) {

    }
}
