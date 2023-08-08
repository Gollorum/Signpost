package gollorum.signpost.minecraft.gui.utils;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public abstract class ExtendedScreen extends Screen {

	protected ExtendedScreen(Component title) {
		super(title);
	}

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}

	@Override
	public void tick() {
		super.tick();
		Streams.concat(children().stream().map(c -> (Object)c), renderables.stream().map(a -> (Object)a))
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

}
