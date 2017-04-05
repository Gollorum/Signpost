package gollorum.signpost.gui;

import java.awt.Color;
import java.io.IOException;

import gollorum.signpost.blocks.BasePostTile;
import gollorum.signpost.management.PostHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.FMLClientHandler;

public class SignGuiBase extends GuiScreen {

	private GuiTextField nameInputBox;
	private BasePostTile tile;
	private boolean textChanged = false;

	public SignGuiBase(BasePostTile tile) {
		this.tile = tile;
		nameInputBox = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 68, this.height / 2 - 46, 137, 20);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if(mc==null){
			mc = FMLClientHandler.instance().getClient();
		}
		drawDefaultBackground();
		nameInputBox.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void initGui() {
		nameInputBox = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 68, this.height / 2 - 46, 137, 20);
		nameInputBox.setMaxStringLength(23);
		nameInputBox.setText(tile.getName());
		nameInputBox.setFocused(true);
	}

	@Override
	protected void keyTyped(char par1, int par2) throws IOException {
		if(par1==13){
			this.mc.displayGuiScreen(null);
			return;
		}
		String before = nameInputBox.getText();
		super.keyTyped(par1, par2);
		this.nameInputBox.textboxKeyTyped(par1, par2);
		if(nameInputBox.getText().equals(tile.getBaseInfo().name)){
			nameInputBox.setTextColor(Color.white.getRGB());
			textChanged = false;
		}else if (!before.equals(nameInputBox.getText())) {
			if (PostHandler.allWaystones.nameTaken(nameInputBox.getText())) {
				nameInputBox.setTextColor(Color.red.getRGB());
				textChanged = false;
			} else {
				nameInputBox.setTextColor(Color.white.getRGB());
				textChanged = true;
			}
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		this.nameInputBox.updateCursorCounter();
	}

	public void updateName(String newName) {
		if (nameInputBox != null) {
			nameInputBox.setText(newName);
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int btn) throws IOException {
		super.mouseClicked(x, y, btn);
		this.nameInputBox.mouseClicked(x, y, btn);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		if (textChanged) {
			tile.setName(nameInputBox.getText());
			textChanged = false;
		}
	}
}
