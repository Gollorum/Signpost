package gollorum.signpost.minecraft.gui;

import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class PaintSignGui<T extends SignBlockPart<T>> extends PaintBlockPartGui<T> {

    private final TextureAtlasSprite oldMainSprite;
    private final TextureAtlasSprite oldSecSprite;

    private boolean isTargetingMainTexture;

    public PaintSignGui(PostTile tile, T sign, UUID identifier) {
        super(tile, sign, sign.copy(), identifier, sign.getMainTexture());
        oldMainSprite = oldSprite;
        oldSecSprite = spriteFrom(sign.getSecondaryTexture());
        isTargetingMainTexture = true;
    }

    public static <T extends SignBlockPart<T>> void display(PostTile tile, T sign, UUID identifier) {
        Minecraft.getInstance().setScreen(new PaintSignGui<>(tile, sign, identifier));
    }

    @Override
    protected void init() {
        super.init();
        Rect button1Rect = new Rect(new Point(width / 4, height / 4), 125, 20, Rect.XAlignment.Center, Rect.YAlignment.Center);
        AtomicReference<Button> b1 = new AtomicReference<>();
        AtomicReference<Button> b2 = new AtomicReference<>();
        b1.set(new Button(
            button1Rect.point.x, button1Rect.point.y,
            button1Rect.width, button1Rect.height,
            new TranslatableComponent(LangKeys.mainTex),
            b -> {
                isTargetingMainTexture = true;
                oldSprite = oldMainSprite;
                clearSelection();
                b1.get().active = false;
                b2.get().active = true;
            }
        ));
        Rect button2Rect = new Rect(button1Rect.max().withY(y -> y + 5), 125, 20, Rect.XAlignment.Right, Rect.YAlignment.Top);
        b2.set(new Button(
            button2Rect.point.x, button2Rect.point.y,
            button2Rect.width, button2Rect.height,
            new TranslatableComponent(LangKeys.secondaryTex),
            b -> {
                isTargetingMainTexture = false;
                oldSprite = oldSecSprite;
                clearSelection();
                b1.get().active = true;
                b2.get().active = false;
            }
        ));
        b1.get().active = !isTargetingMainTexture;
        b2.get().active = isTargetingMainTexture;
        addRenderableWidget(b1.get());
        addRenderableWidget(b2.get());
    }

    @Override
    protected void setTexture(T sign, ResourceLocation texture) {
        if(isTargetingMainTexture) sign.setMainTexture(texture);
        else sign.setSecondaryTexture(texture);
    }

}
