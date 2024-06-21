package gollorum.signpost.mixin;

import gollorum.signpost.minecraft.gui.utils.IColorableButton;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractButton.class)
abstract class ColorableButton extends AbstractWidget implements IColorableButton {

    @Unique
    private int signpost$colorOverride = ~0;

    private ColorableButton(int $$0, int $$1, int $$2, int $$3, Component $$4) {
        super($$0, $$1, $$2, $$3, $$4);
    }

    @Unique
    public void signpost$overrideColor(int color) {
        signpost$colorOverride = color;
    }

    @ModifyVariable(method = "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("STORE"), ordinal = 2)
    private int injected(int color) {
        if(signpost$colorOverride == ~0) return color;
        else return signpost$colorOverride;
    }
}
