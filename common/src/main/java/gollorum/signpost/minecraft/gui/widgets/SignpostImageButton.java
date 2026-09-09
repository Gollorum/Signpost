package gollorum.signpost.minecraft.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.CommonComponents;

public class SignpostImageButton extends Button {

    private final TextureResource background;

    public SignpostImageButton(
        TextureResource background,
        Point point,
        float scale,
        Rect.XAlignment xAlignment,
        Rect.YAlignment yAlignment,
        Runnable onPress
    ) {
        this(
            background,
            new Rect(point, background.size.scale(scale), xAlignment, yAlignment),
            b -> onPress.run()
        );
    }

    public SignpostImageButton(
        TextureResource background,
        Rect rect,
        Button.OnPress onPress
    ){
        super(
            rect.point.x, rect.point.y,
            rect.width, rect.height,
            CommonComponents.EMPTY,
            onPress,
            DEFAULT_NARRATION
        );
        this.background = background;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            background.location,
            this.getX(), this.getY(),
            background.offset.width, background.offset.height + (this.isHoveredOrFocused() ? background.size.height : 0),
            this.width, this.height,
            background.size.width, background.size.height,
            background.fileSize.width, background.fileSize.height);
    }

}
