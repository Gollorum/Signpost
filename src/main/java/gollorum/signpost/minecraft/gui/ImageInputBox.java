package gollorum.signpost.minecraft.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public final class ImageInputBox extends InputBox {

    private final TextureResource texture;
    private final Rect backgroundRect;

    public final Rect bounds;

    public ImageInputBox(
        FontRenderer fontRenderer,
        Rect inputFieldRect,
        Rect backgroundRect,
        Rect.XAlignment backXAlignment,
        Rect.YAlignment backYAlignment,
        TextureResource texture,
        boolean shouldDropShadow
    ) {
        super(fontRenderer,
            inputFieldRect,
            shouldDropShadow,
            false
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
//        this.backgroundRect = new Rect(inputFieldRect.point.add(new Point(inputFieldRect.width / 2, inputFieldRect.height / 2)).add(backgroundRect.point),
//            backgroundRect.width, backgroundRect.height);
        bounds = new Rect(
            Point.min(inputFieldRect.min(), this.backgroundRect.min()),
            Point.max(inputFieldRect.max(), this.backgroundRect.max())
        );
    }

    @Override
    public void renderButton(int p_render_1_, int p_render_2_, float p_render_3_) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        Minecraft.getInstance().getTextureManager().bindTexture(texture.location);
        blit(backgroundRect.point.x, backgroundRect.point.y, 0, 0, backgroundRect.width, backgroundRect.height, backgroundRect.width, backgroundRect.height);
        super.renderButton(p_render_1_, p_render_2_, p_render_3_);
    }

}
