package gollorum.signpost.minecraft.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import gollorum.signpost.minecraft.gui.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public final class ImageInputBox extends InputBox implements Flippable {

    private final TextureResource texture;
    private Rect backgroundRect;

    public final Rect bounds;

    public ImageInputBox(
        FontRenderer fontRenderer,
        Rect inputFieldRect,
        Rect backgroundRect,
        Rect.XAlignment backXAlignment,
        Rect.YAlignment backYAlignment,
        TextureResource texture,
        boolean shouldDropShadow,
        double zOffset
    ) {
        super(
            fontRenderer,
            inputFieldRect,
            shouldDropShadow,
            false,
            zOffset
        );
        this.texture = texture;
        setEnableBackgroundDrawing(false);
        setTextColor(Colors.black);
        int x = backgroundRect.point.x + inputFieldRect.point.x;
        switch(backXAlignment) {
            case Center: x += inputFieldRect.width / 2; break;
            case Right: x += inputFieldRect.width; break;
        }
        int y = backgroundRect.point.y + inputFieldRect.point.y;
        switch(backYAlignment) {
            case Center: y += inputFieldRect.height / 2; break;
            case Bottom: y += inputFieldRect.height; break;
        }
        this.backgroundRect = new Rect(new Point(x, y), backgroundRect.width, backgroundRect.height);
        bounds = new Rect(
            Point.min(inputFieldRect.min(), this.backgroundRect.min()),
            Point.max(inputFieldRect.max(), this.backgroundRect.max())
        );
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        Minecraft.getInstance().getTextureManager().bindTexture(texture.location);
        blit(matrixStack, backgroundRect.point.x, backgroundRect.point.y, 0, 0, backgroundRect.width, backgroundRect.height, isFlipped ? -backgroundRect.width : backgroundRect.width, backgroundRect.height);

        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
    }

    private boolean isFlipped = false;

    public boolean isFlipped() {
        return isFlipped;
    }

    public void setFlipped(boolean isFlipped) {
        if(isFlipped != this.isFlipped)
            backgroundRect = backgroundRect.withPoint(p -> p.withX(oldX -> x + (x + width) - (backgroundRect.point.x + backgroundRect.width)));
        this.isFlipped = isFlipped;
    }

}
