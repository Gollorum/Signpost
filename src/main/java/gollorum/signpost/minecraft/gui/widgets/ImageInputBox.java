package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import gollorum.signpost.minecraft.gui.utils.*;
import net.minecraft.client.gui.Font;

public final class ImageInputBox extends InputBox implements Flippable {

    private final TextureResource texture;
    private Rect backgroundRect;

    public final Rect bounds;

    public ImageInputBox(
        Font fontRenderer,
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
            zOffset
        );
        this.texture = texture;
        setBordered(false);
        setTextColor(Colors.black);
        int x = backgroundRect.point.x + inputFieldRect.point.x;
        switch (backXAlignment) {
            case Center -> x += inputFieldRect.width / 2;
            case Right -> x += inputFieldRect.width;
        }
        int y = backgroundRect.point.y + inputFieldRect.point.y;
        switch (backYAlignment) {
            case Center -> y += inputFieldRect.height / 2;
            case Bottom -> y += inputFieldRect.height;
        }
        this.backgroundRect = new Rect(new Point(x, y), backgroundRect.width, backgroundRect.height);
        bounds = new Rect(
            Point.min(inputFieldRect.min(), this.backgroundRect.min()),
            Point.max(inputFieldRect.max(), this.backgroundRect.max())
        );
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, texture.location);
        RenderSystem.enableDepthTest();
        blit(matrixStack, backgroundRect.point.x, backgroundRect.point.y, 0, 0, backgroundRect.width, backgroundRect.height, isFlipped ? -backgroundRect.width : backgroundRect.width, backgroundRect.height);

        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
    }

    private boolean isFlipped = false;

    public boolean isFlipped() {
        return isFlipped;
    }

    public void setFlipped(boolean isFlipped) {
        if(isFlipped != this.isFlipped)
            backgroundRect = backgroundRect.withPoint(p -> p.withX(oldX -> getX() + (getX() + width) - (backgroundRect.point.x + backgroundRect.width)));
        this.isFlipped = isFlipped;
    }

}
