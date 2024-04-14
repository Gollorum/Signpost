package gollorum.signpost.minecraft.gui.utils;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public class ConfigurableFont {

    @Unique
    private boolean shouldProhibitShadows = false;

    @Unique
    public void setShouldProhibitShadows(boolean shouldProhibitShadows) {
        this.shouldProhibitShadows = shouldProhibitShadows;
    }

    @ModifyVariable(method = "drawInternal*", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean injectShadowProhibition(boolean drawShadow) {
        return drawShadow && !shouldProhibitShadows;
    }
}
