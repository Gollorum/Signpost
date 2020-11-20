package gollorum.signpost.minecraft.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import gollorum.signpost.Signpost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.ResourceLocation;

public final class WaystoneInputBox extends TextFieldWidget {

    private static final ResourceLocation texture =
        new ResourceLocation(Signpost.MOD_ID, "textures/gui/base_gui.png");

    public static final int textureWidth = 25;
    public static final int textureHeight = 6;

    private static final int offset = 2;

    public static final float widthHeightRatio = textureWidth / (float) textureHeight;

    private final float guiToTextureScale;

    private final int backgroundX;
    private final int backgroundY;
    private final int backgroundWidth;

    public WaystoneInputBox(int x, int y, int width) {
        this(x, y, width, textureWidth / (float) width);
    }

    private WaystoneInputBox(int x, int y, int width, float guiToTextureScale) {
        super(Minecraft.getInstance().fontRenderer,
            x + (int)(offset / guiToTextureScale),
            y + (int)((textureHeight / 2 - 0.5f) / guiToTextureScale),
            width - (int)(2 * offset / guiToTextureScale),
            Math.round(width / widthHeightRatio),
            "Input Waystone Name");
        setEnableBackgroundDrawing(false);
        setTextColor(Colors.black);
        backgroundX = x;
        backgroundY = y;
        backgroundWidth = width;
        this.guiToTextureScale = guiToTextureScale;
    }

    @Override
    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bindTexture(texture);
        blit(backgroundX, backgroundY, 0, 0, backgroundWidth, height, backgroundWidth, height);
        super.render(p_render_1_, p_render_2_, p_render_3_);
    }
}
