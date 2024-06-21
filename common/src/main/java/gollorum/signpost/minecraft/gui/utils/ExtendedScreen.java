package gollorum.signpost.minecraft.gui.utils;

import com.google.common.collect.Streams;
import gollorum.signpost.mixin.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public abstract class ExtendedScreen extends Screen {

	protected ExtendedScreen(Component title) {
		super(title);
	}

//	@Override
//	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
//		renderBackground(graphics, mouseX, mouseY, partialTicks);
//		super.render(graphics, mouseX, mouseY, partialTicks);
//	}

	@Override
	public void tick() {
		super.tick();
		Streams.concat(children().stream().map(c -> (Object)c), ((ScreenAccessor)this).getRenderables().stream().map(a -> (Object)a))
			.filter(o -> o instanceof Ticking)
			.map(o -> (Ticking) o)
			.distinct()
			.forEach(Ticking::doTick);
	}

	@Override
	public void setFocused(@Nullable GuiEventListener listener) {
		if(getFocused() != listener && getFocused() instanceof AbstractWidget oldListener) {
			if(oldListener.isFocused()) oldListener.setFocused(false);
		}
		super.setFocused(listener);
	}

    protected Minecraft minecraft() {
        return minecraft;
    }

}
