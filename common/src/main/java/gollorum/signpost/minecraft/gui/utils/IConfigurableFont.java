package gollorum.signpost.minecraft.gui.utils;

import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Duck interface mixed into {@link net.minecraft.client.gui.Font} by
 * {@code gollorum.signpost.mixin.ConfigurableFont}.
 *
 * <p>1.21.1's {@code EditBox} always draws its contents through the shadowed
 * {@code GuiGraphics.drawString} overload and offers no {@code setTextShadow}, so suppressing the
 * shadow has to happen inside the font. The two getters exist so a widget can build its <em>own</em>
 * {@code Font} out of the shared one's state - prohibiting shadows on Minecraft's font instance would
 * strip them from every piece of text in the game.
 */
public interface IConfigurableFont {

    void setShouldProhibitShadows(boolean shouldProhibitShadows);

    Function<ResourceLocation, FontSet> getFonts();

    boolean getFilterFishyGlyphs();

}
