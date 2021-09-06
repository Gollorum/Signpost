package gollorum.signpost.minecraft.gui.utils;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;

public class ConfigurableFont extends FontRenderer {

    private boolean shouldProhibitShadows;

    public ConfigurableFont(FontRenderer font, boolean shouldProhibitShadows) {
        super(font.fonts);
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    public void setShouldProhibitShadows(boolean shouldProhibitShadows) {
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    @Override
    public int drawShadow(MatrixStack p_92751_, String p_92752_, float p_92753_, float p_92754_, int p_92755_) {
        return shouldProhibitShadows
            ? super.draw(p_92751_, p_92752_, p_92753_, p_92754_, p_92755_)
            : super.drawShadow(p_92751_, p_92752_, p_92753_, p_92754_, p_92755_);
    }

    @Override
    public int drawShadow(MatrixStack p_92757_, String p_92758_, float p_92759_, float p_92760_, int p_92761_, boolean p_92762_) {
        return shouldProhibitShadows
            ? super.draw(p_92757_, p_92758_, p_92759_, p_92760_, p_92761_)
            : super.drawShadow(p_92757_, p_92758_, p_92759_, p_92760_, p_92761_, p_92762_);
    }

    @Override
    public int drawShadow(MatrixStack p_92745_, IReorderingProcessor p_92746_, float p_92747_, float p_92748_, int p_92749_) {
        return shouldProhibitShadows
            ? super.draw(p_92745_, p_92746_, p_92747_, p_92748_, p_92749_)
            : super.drawShadow(p_92745_, p_92746_, p_92747_, p_92748_, p_92749_);
    }

    @Override
    public int drawShadow(MatrixStack p_92764_, ITextComponent p_92765_, float p_92766_, float p_92767_, int p_92768_) {
        return shouldProhibitShadows
            ? super.draw(p_92764_, p_92765_, p_92766_, p_92767_, p_92768_)
            : super.drawShadow(p_92764_, p_92765_, p_92766_, p_92767_, p_92768_);
    }
}
