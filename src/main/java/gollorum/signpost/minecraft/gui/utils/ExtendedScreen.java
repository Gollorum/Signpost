package gollorum.signpost.minecraft.gui.utils;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ExtendedScreen extends Screen {

	protected final List<IRenderable> additionalRenderables = new ArrayList<>();

	protected ExtendedScreen(ITextComponent titleIn) {
		super(titleIn);
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(matrixStack);
		for(IRenderable toRender : additionalRenderables) {
			toRender.render(matrixStack, mouseX, mouseY, partialTicks);
		}
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}

	protected void addButtons(Collection<? extends Widget> widgets) {
		this.buttons.addAll(widgets);
		this.children.addAll(widgets);
	}

	protected void removeButtons(Collection<? extends Widget> widgets) {
		this.buttons.removeAll(widgets);
		this.children.removeAll(widgets);
	}

	protected void removeButton(Widget widget) {
		this.buttons.remove(widget);
		this.children.remove(widget);
	}

	@Override
	public void tick() {
		super.tick();
		Streams.concat(children.stream().map(c -> (Object)c), additionalRenderables.stream().map(a -> (Object)a))
			.filter(o -> o instanceof Ticking)
			.map(o -> (Ticking) o)
			.forEach(Ticking::doTick);
	}

	@Override
	public void setFocused(@Nullable IGuiEventListener listener) {
		if(getFocused() != listener && getFocused() instanceof Widget) {
			Widget oldListener = (Widget) getFocused();
			if(oldListener.isFocused()) oldListener.changeFocus(false);
		}
		super.setFocused(listener);
	}

	@Override
	protected void init() {
		super.init();
		additionalRenderables.clear();
	}
}
