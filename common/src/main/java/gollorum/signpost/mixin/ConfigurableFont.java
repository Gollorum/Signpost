package gollorum.signpost.mixin;

import gollorum.signpost.minecraft.gui.utils.IConfigurableFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Function;

@Mixin(Font.class)
public abstract class ConfigurableFont implements IConfigurableFont {

    @Unique
    private boolean shouldProhibitShadows = false;

    @Unique
    @Override
    public void setShouldProhibitShadows(boolean shouldProhibitShadows) {
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    @Accessor
    @Override
    public abstract Function<ResourceLocation, FontSet> getFonts();

    @Accessor
    @Override
    public abstract boolean getFilterFishyGlyphs();

    @ModifyVariable(method = "drawInternal*", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean injectShadowProhibition(boolean drawShadow) {
        return drawShadow && !shouldProhibitShadows;
    }
}
