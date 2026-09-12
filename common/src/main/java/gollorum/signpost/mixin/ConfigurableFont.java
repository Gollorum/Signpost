package gollorum.signpost.mixin;

import gollorum.signpost.minecraft.gui.utils.IConfigurableFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Function;

/**
 * Lets a single {@link Font} instance be told to draw without shadows.
 *
 * <p>Both {@code drawInternal} overloads take {@code dropShadow} as their first boolean argument, so
 * one wildcard injector covers them and every {@code drawInBatch} / {@code GuiGraphics.drawString}
 * path funnels through it. Only fonts a widget has explicitly opted in are affected; the flag
 * defaults to false, so Minecraft's shared font is untouched.
 *
 * @see IConfigurableFont
 */
@Mixin(Font.class)
public abstract class ConfigurableFont implements IConfigurableFont {

    @Unique
    private boolean signpost$shouldProhibitShadows = false;

    @Unique
    @Override
    public void setShouldProhibitShadows(boolean shouldProhibitShadows) {
        this.signpost$shouldProhibitShadows = shouldProhibitShadows;
    }

    @Accessor
    @Override
    public abstract Function<ResourceLocation, FontSet> getFonts();

    @Accessor
    @Override
    public abstract boolean getFilterFishyGlyphs();

    // Both overloads are named explicitly rather than matched with "drawInternal*": EditBox draws its
    // contents through the FormattedCharSequence one, and a wildcard resolves to a single entry in the
    // Fabric refmap, which would silently leave that overload uninjected.
    @ModifyVariable(
        method = {
            "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
            "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I"
        },
        at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean signpost$injectShadowProhibition(boolean dropShadow) {
        return dropShadow && !signpost$shouldProhibitShadows;
    }
}
