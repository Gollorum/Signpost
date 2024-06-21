package gollorum.signpost.minecraft.gui.utils;

import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public interface IConfigurableFont {

    void setShouldProhibitShadows(boolean shouldProhibitShadows);

    Function<ResourceLocation, FontSet> getFonts();

    boolean getFilterFishyGlyphs();

}
