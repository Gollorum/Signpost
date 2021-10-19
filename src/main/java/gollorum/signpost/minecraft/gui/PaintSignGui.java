package gollorum.signpost.minecraft.gui;

import gollorum.signpost.blockpartdata.types.SignBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.utils.LangKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.UUID;

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
        Rect buttonRect = new Rect(new Point(width / 4, height / 4), 125, 20, Rect.XAlignment.Center, Rect.YAlignment.Center);
        addButton(new Button(
            buttonRect.point.x, buttonRect.point.y,
            buttonRect.width, buttonRect.height,
            new TranslationTextComponent(isTargetingMainTexture ? LangKeys.mainTex : LangKeys.secondaryTex),
            b -> {
                isTargetingMainTexture = !isTargetingMainTexture;
                oldSprite = isTargetingMainTexture ? oldMainSprite : oldSecSprite;
                clearSelection();
                b.setMessage(new TranslationTextComponent(isTargetingMainTexture ? LangKeys.mainTex : LangKeys.secondaryTex));
            }
        ));
    }

    @Override
    protected void setTexture(T sign, ResourceLocation texture) {
        if(isTargetingMainTexture) sign.setMainTexture(texture);
        else sign.setSecondaryTexture(texture);
    }

}
