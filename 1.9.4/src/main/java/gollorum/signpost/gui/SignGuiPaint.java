package gollorum.signpost.gui;

import java.io.IOException;

import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.SuperPostPostTile;
import gollorum.signpost.util.Sign;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;

public class SignGuiPaint extends GuiScreen {

	private GuiTextField nameInputBox;
	private Sign sign;
	private SuperPostPostTile tile;

	public SignGuiPaint(Sign sign, SuperPostPostTile tile) {
		this.sign = sign;
		this.tile = tile;
		initGui();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if(mc==null){
			mc = FMLClientHandler.instance().getClient();
		}
		if(sign==null){
			this.mc.displayGuiScreen(null);
			return;
		}
		if(nameInputBox==null){
			initGui();
		}
		drawDefaultBackground();
		if(nameInputBox.getText() == null || nameInputBox.getText().equals("null")){
			ResourceLocation loc = sign.paint;
			String name;
			if(loc==null){
				name = "";
			}else{
				name = SuperPostPostTile.LocToString(loc);
			}
			nameInputBox.setText(name);
		}
		nameInputBox.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void initGui() {
		nameInputBox = new GuiTextField(0, this.fontRendererObj, this.width/4, this.height/2 - 46, this.width/2, 20);
		nameInputBox.setMaxStringLength(100);
		if(mc==null){
			mc = FMLClientHandler.instance().getClient();
		}
		if(sign==null){
			this.mc.displayGuiScreen(null);
			return;
		}
		ResourceLocation loc = sign.paint;
		String name;
		if(loc==null){
			name = "";
		}else{
			name = SuperPostPostTile.LocToString(loc);
		}
		nameInputBox.setText(name);
		nameInputBox.setFocused(true);
	}

	@Override
	protected void keyTyped(char par1, int par2) throws IOException {
		if(par1==13){
			this.mc.displayGuiScreen(null);
			return;
		}
		super.keyTyped(par1, par2);
		this.nameInputBox.textboxKeyTyped(par1, par2);
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
		if(sign!=null){
			sign.paint = new ResourceLocation(nameInputBox.getText());
			((SuperPostPost)tile.getBlockType()).sendPostBasesToAll(tile);
		}
	}
}
