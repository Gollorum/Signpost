package gollorum.signpost.minecraft.gui.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

public class ConfigurableFont extends Font {

    private boolean shouldProhibitShadows;

    public ConfigurableFont(Font font, boolean shouldProhibitShadows) {
        super(font.fonts, font.filterFishyGlyphs);
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    public void setShouldProhibitShadows(boolean shouldProhibitShadows) {
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    @Override
    public float renderText(String p_273765_, float p_273532_, float p_272783_, int p_273217_, boolean p_273583_, Matrix4f p_272734_, MultiBufferSource p_272595_, DisplayMode p_273610_, int p_273727_, int p_273199_) {
        return super.renderText(p_273765_, p_273532_, p_272783_, p_273217_, !shouldProhibitShadows && p_273583_, p_272734_, p_272595_, p_273610_, p_273727_, p_273199_);
    }

    @Override
    public float renderText(FormattedCharSequence p_273322_, float p_272632_, float p_273541_, int p_273200_, boolean p_273312_, Matrix4f p_273276_, MultiBufferSource p_273392_, DisplayMode p_272625_, int p_273774_, int p_273371_) {
        return super.renderText(p_273322_, p_272632_, p_273541_, p_273200_, !shouldProhibitShadows && p_273312_, p_273276_, p_273392_, p_272625_, p_273774_, p_273371_);
    }
}
