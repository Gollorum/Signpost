package gollorum.signpost.minecraft.gui.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Field;
import java.util.function.Function;

public class ConfigurableFont extends Font {

    private boolean shouldProhibitShadows;

    private static Field fontsField;

    static {
        try {
            fontsField = Font.class.getDeclaredField("fonts");
            fontsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static Function<ResourceLocation, FontSet> getFonts(Font font) {
        try {
            return (Function<ResourceLocation, FontSet>) fontsField.get(font);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e); // I know. Don't judge me.
        }
    }

    public ConfigurableFont(Font font, boolean shouldProhibitShadows) {
        super(getFonts(font));
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    public void setShouldProhibitShadows(boolean shouldProhibitShadows) {
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    @Override
    public int drawShadow(PoseStack p_92751_, String p_92752_, float p_92753_, float p_92754_, int p_92755_) {
        return shouldProhibitShadows
            ? super.draw(p_92751_, p_92752_, p_92753_, p_92754_, p_92755_)
            : super.drawShadow(p_92751_, p_92752_, p_92753_, p_92754_, p_92755_);
    }

    @Override
    public int drawShadow(PoseStack p_92757_, String p_92758_, float p_92759_, float p_92760_, int p_92761_, boolean p_92762_) {
        return shouldProhibitShadows
            ? super.draw(p_92757_, p_92758_, p_92759_, p_92760_, p_92761_)
            : super.drawShadow(p_92757_, p_92758_, p_92759_, p_92760_, p_92761_, p_92762_);
    }

    @Override
    public int drawShadow(PoseStack p_92745_, FormattedCharSequence p_92746_, float p_92747_, float p_92748_, int p_92749_) {
        return shouldProhibitShadows
            ? super.draw(p_92745_, p_92746_, p_92747_, p_92748_, p_92749_)
            : super.drawShadow(p_92745_, p_92746_, p_92747_, p_92748_, p_92749_);
    }

    @Override
    public int drawShadow(PoseStack p_92764_, Component p_92765_, float p_92766_, float p_92767_, int p_92768_) {
        return shouldProhibitShadows
            ? super.draw(p_92764_, p_92765_, p_92766_, p_92767_, p_92768_)
            : super.drawShadow(p_92764_, p_92765_, p_92766_, p_92767_, p_92768_);
    }
}
