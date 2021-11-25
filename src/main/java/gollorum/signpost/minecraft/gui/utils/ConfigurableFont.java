package gollorum.signpost.minecraft.gui.utils;

import net.minecraft.client.gui.FontRenderer;

public class ConfigurableFont extends FontRenderer {

    private boolean shouldProhibitShadows;

    public ConfigurableFont(FontRenderer font, boolean shouldProhibitShadows) {
        super(font.textureManager, font.fonts);
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    public void setShouldProhibitShadows(boolean shouldProhibitShadows) {
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    @Override
    public int drawShadow(String p_92752_, float p_92753_, float p_92754_, int p_92755_) {
        return shouldProhibitShadows
            ? super.draw(p_92752_, p_92753_, p_92754_, p_92755_)
            : super.drawShadow(p_92752_, p_92753_, p_92754_, p_92755_);
    }

}
