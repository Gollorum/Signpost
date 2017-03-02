package gollorum.signpost.gui;

import java.awt.Color;
import java.io.IOException;

import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.settings.GameSettings.Options;

public class SignGuiPost extends GuiScreen {

	private GuiTextField base2InputBox;
	private GuiTextField base1InputBox;
	
	private PostPostTile tile;
	
	public SignGuiPost(PostPostTile tile) {
		this.tile = tile;
	}

	public void initGui() {
		base2InputBox = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 68, this.height / 2 - 46, 137, 20);
		base2InputBox.setMaxStringLength(23);
		base2InputBox.setText(tile.bases.base1==null?"":tile.bases.base1.toString());
		base1InputBox = new GuiTextField(1, this.fontRendererObj, this.width / 2 - 68, this.height / 2, 137, 20);
		base1InputBox.setMaxStringLength(23);
		base1InputBox.setText(tile.bases.base2==null?"":tile.bases.base2.toString());
	}
	
	@Override
    protected void mouseClicked(int x, int y, int bla) throws IOException{
		super.mouseClicked(x, y, bla);
		base2InputBox.mouseClicked(x, y, bla);
		base1InputBox.mouseClicked(x, y, bla);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		base2InputBox.drawTextBox();
		base1InputBox.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void keyTyped(char par1, int par2) throws IOException {
		super.keyTyped(par1, par2);
		baseType(par1, par2, false);
		baseType(par1, par2, true);
	}
	
	private void baseType(char par1, int par2, boolean base2){
		GuiTextField tf = base2?base1InputBox:base2InputBox;
		String before = tf.getText();
		if(tf.textboxKeyTyped(par1, par2)&&!tf.getText().equals(before)){
			BaseInfo inf = PostHandler.getWSbyName(tf.getText());
			if(inf==null){
				tf.setTextColor(Color.red.getRGB());
			}else{
				tf.setTextColor(Color.white.getRGB());
			}
		}
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		tile.bases.base2 = PostHandler.getWSbyName(base1InputBox.getText());
		tile.bases.base1 = PostHandler.getWSbyName(base2InputBox.getText());
		NetworkHandler.netWrap.sendToServer(new SendPostBasesMessage(tile.toPos(), tile.bases));
	}
}
