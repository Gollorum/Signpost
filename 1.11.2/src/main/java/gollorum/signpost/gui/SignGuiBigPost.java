package gollorum.signpost.gui;

import java.awt.Color;
import java.io.IOException;

import gollorum.signpost.blocks.BigPostPostTile;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendBigPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.MyBlockPos.Connection;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;

public class SignGuiBigPost extends GuiScreen {

	private SignInputBox baseInputBox;

	private GuiTextField desc1InputBox;
	private GuiTextField desc2InputBox;
	private GuiTextField desc3InputBox;
	private GuiTextField desc4InputBox;
	
	private String std = "";
	private int col = 0;
	private boolean go;
	
	private BigPostPostTile tile;

	private boolean resetMouse;
	
	public SignGuiBigPost(BigPostPostTile tile) {
		this.tile = tile;
		baseInputBox = new SignInputBox(this.fontRendererObj, this.width / 2 - 68, 46, 137);

		desc1InputBox = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 68, 106, 137, 20);
		desc2InputBox = new GuiTextField(1, this.fontRendererObj, this.width / 2 - 68, 136, 137, 20);
		desc3InputBox = new GuiTextField(2, this.fontRendererObj, this.width / 2 - 68, 166, 137, 20);
		desc4InputBox = new GuiTextField(3, this.fontRendererObj, this.width / 2 - 68, 196, 137, 20);
	}

	@Override
	public void initGui() {
		BigBaseInfo tilebases = tile.getBases();
		baseInputBox = new SignInputBox(this.fontRendererObj, this.width / 2 - 68, 46, 137);
		baseInputBox.setText(tilebases.sign.base==null?"":tilebases.sign.base.toString());
		go = true;
		baseInputBox.setFocused(true);

		desc1InputBox = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 68, 106, 137, 20);
		desc1InputBox.setText(""+tilebases.description[0]);
		desc2InputBox = new GuiTextField(1, this.fontRendererObj, this.width / 2 - 68, 136, 137, 20);
		desc2InputBox.setText(""+tilebases.description[1]);
		desc3InputBox = new GuiTextField(2, this.fontRendererObj, this.width / 2 - 68, 166, 137, 20);
		desc3InputBox.setText(""+tilebases.description[2]);
		desc4InputBox = new GuiTextField(3, this.fontRendererObj, this.width / 2 - 68, 196, 137, 20);
		desc4InputBox.setText(""+tilebases.description[3]);
		
		resetMouse = true;
	}
	
	@Override
    protected void mouseClicked(int x, int y, int bla) throws IOException{
		super.mouseClicked(x, y, bla);
		baseInputBox.mouseClicked(x, y, bla);
		desc1InputBox.mouseClicked(x, y, bla);
		desc2InputBox.mouseClicked(x, y, bla);
		desc3InputBox.mouseClicked(x, y, bla);
		desc4InputBox.mouseClicked(x, y, bla);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if(mc==null){
			mc = FMLClientHandler.instance().getClient();
		}
		drawDefaultBackground();
		baseInputBox.drawSignBox(fontRendererObj);
		this.drawCenteredString(fontRendererObj, std, this.width/2, baseInputBox.y+baseInputBox.height+10, col);
		desc1InputBox.drawTextBox();
		desc2InputBox.drawTextBox();
		desc3InputBox.drawTextBox();
		desc4InputBox.drawTextBox();
		if(resetMouse){
			resetMouse = false;
			org.lwjgl.input.Mouse.setGrabbed(false);
		}
	}

	@Override
	protected void keyTyped(char par1, int par2) throws IOException {
		super.keyTyped(par1, par2);
		if(par1==13){
			if(baseInputBox.isFocused()){
				if(!go){
					go=true;
					baseInputBox.textColor = Color.orange.getRGB();
				}
				this.mc.displayGuiScreen(null);
			}else{
				baseInputBox.setFocused(true);
			}
			return;
		}else if(par1==9){
			if(!baseInputBox.isFocused()){
				baseInputBox.setFocused(true);
			}
			return;
		}
		baseType(par1, par2);
		desc1InputBox.textboxKeyTyped(par1, par2);
		desc2InputBox.textboxKeyTyped(par1, par2);
		desc3InputBox.textboxKeyTyped(par1, par2);
		desc4InputBox.textboxKeyTyped(par1, par2);
	}
	
	private void baseType(char par1, int par2){
		SignInputBox tf = baseInputBox;
		String before = tf.getText();
		if(tf.textboxKeyTyped(par1, par2)&&!tf.getText().equals(before)){
			if(ConfigHandler.deactivateTeleportation){
				return;
			}
			BaseInfo inf = PostHandler.getWSbyName(tf.getText());
			Connection connect = tile.toPos().canConnectTo(inf);
			if(inf==null||!connect.equals(Connection.VALID)){
				tf.setTextColor(Color.red.getRGB());
				if(connect.equals(Connection.DIST)){
					
					String out = I18n.translateToLocal("signpost.guiTooFar");
					out = out.replaceAll("<distance>", ""+(int)tile.toPos().distance(inf.pos)+1);
					out = out.replaceAll("<maxDist>", ""+ConfigHandler.maxDist);
					std = out;
					col = Color.red.getRGB();
					go = false;
					
				}else if(connect.equals(Connection.WORLD)){

					String out = I18n.translateToLocal("signpost.guiWorldDim");
					std = out;
					col = Color.red.getRGB();
					go = false;
					
				}else{
					std = "";
					col = Color.red.getRGB();
					go = false;
				}
			}else{
				tf.setTextColor(Color.black.getRGB());
				col = Color.white.getRGB();
				go = true;

				if(!(ConfigHandler.deactivateTeleportation||ConfigHandler.cost==null)){
					String out = I18n.translateToLocal("signpost.guiPrev");
					int distance = (int) tile.toPos().distance(inf.pos)+1;
					out = out.replaceAll("<distance>", ""+distance);
					out = out.replaceAll("<amount>", Integer.toString((int) (tile.toPos().distance(inf.pos)/ConfigHandler.costMult+1)));
					out = out.replaceAll("<itemName>", ConfigHandler.costName());
					col = Color.white.getRGB();
					std = out;
				}
			}
		}
	}

	@Override
	public void onGuiClosed() {
		BigBaseInfo tilebases = tile.getBases();
		if(ConfigHandler.deactivateTeleportation||go){
			tilebases.sign.base = PostHandler.getForceWSbyName(baseInputBox.getText());
		}else{
			tilebases.sign.base = null;
		}
		tilebases.description[0] = desc1InputBox.getText();
		tilebases.description[1] = desc2InputBox.getText();
		tilebases.description[2] = desc3InputBox.getText();
		tilebases.description[3] = desc4InputBox.getText();
		NetworkHandler.netWrap.sendToServer(new SendBigPostBasesMessage(tile, tilebases));
	}
}
