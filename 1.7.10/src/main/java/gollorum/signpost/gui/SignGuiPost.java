package gollorum.signpost.gui;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.registry.LanguageRegistry;
import gollorum.signpost.blocks.PostPostTile;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos.Connection;
import gollorum.signpost.util.DoubleBaseInfo;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;

public class SignGuiPost extends GuiScreen {

	private SignInputBox base1InputBox;
	private SignInputBox base2InputBox;
	
	private String std1 = "";
	private int col1 = Color.black.getRGB();
	private boolean go1;
	
	private String std2 = "";
	private int col2 = Color.black.getRGB();
	private boolean go2;
	
	private PostPostTile tile;

	private boolean resetMouse;
	
	public SignGuiPost(PostPostTile tile) {
		this.tile = tile;
	}

	@Override
	public void initGui() {
		DoubleBaseInfo tilebases = tile.getBases();GuiTextField g;
		base1InputBox = new SignInputBox(this.fontRendererObj, this.width / 2 - 68, this.height / 2 - 46, 137/*, 20*/);
		base1InputBox.setText(tilebases.sign1.base==null?"":tilebases.sign1.base.toString());
		go1 = true;
		base1InputBox.setFocused(true);
		base2InputBox = new SignInputBox(this.fontRendererObj, this.width / 2 - 68, this.height / 2 + 40, 137/*, 20*/);
		base2InputBox.setText(tilebases.sign2.base==null?"":tilebases.sign2.base.toString());
		go2 = true;
		resetMouse = true;
	}
	
	@Override
    protected void mouseClicked(int x, int y, int bla){
		super.mouseClicked(x, y, bla);
		base1InputBox.mouseClicked(x, y, bla);
		base2InputBox.mouseClicked(x, y, bla);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		if(mc==null){
			mc = FMLClientHandler.instance().getClient();
		}
		drawDefaultBackground();

		base1InputBox.drawSignBox(fontRendererObj);
		this.drawCenteredString(fontRendererObj, std2, this.width/2, base1InputBox.y+base1InputBox.height+10, col2);

		base2InputBox.drawSignBox(fontRendererObj);
		this.drawCenteredString(fontRendererObj, std1, this.width/2, base2InputBox.y+base2InputBox.height+10, col1);
		
		if(resetMouse){
			resetMouse = false;
			org.lwjgl.input.Mouse.setGrabbed(false);
		}
	}

	@Override
	protected void keyTyped(char par1, int par2) {
		super.keyTyped(par1, par2);
		if(par1==13){
			if(base1InputBox.isFocused()){
				if(!go2){
					go2=true;
					base1InputBox.textColor = Color.orange.getRGB();
				}
				base1InputBox.setFocused(false);
				base2InputBox.setFocused(true);
			}else if(base2InputBox.isFocused()){
				if(!go1){
					go1=true;
					base2InputBox.textColor = Color.orange.getRGB();
				}
				this.mc.displayGuiScreen(null);
			}else{
				base1InputBox.setFocused(true);
			}
			return;
		}else if(par1==9){
			if(base1InputBox.isFocused()){
				base1InputBox.setFocused(false);
				base2InputBox.setFocused(true);
			}else if(base2InputBox.isFocused()){
				base2InputBox.setFocused(false);
				base1InputBox.setFocused(true);
			}else{
				base1InputBox.setFocused(true);
			}
			return;
		}
		baseType(par1, par2, false);
		baseType(par1, par2, true);
	}
	
	private void baseType(char par1, int par2, boolean base2){
		SignInputBox tf = base2?base2InputBox:base1InputBox;
		String before = tf.getText();
		if(tf.textboxKeyTyped(par1, par2)&&!tf.getText().equals(before)){
			if(ConfigHandler.deactivateTeleportation){
				return;
			}
			BaseInfo inf = PostHandler.getWSbyName(tf.getText());
			Connection connect = tile.toPos().canConnectTo(inf);
			if(inf==null||!connect.equals(Connection.VALID)){
				tf.textColor = Color.red.getRGB();
				if(connect.equals(Connection.DIST)){
					
					String out = LanguageRegistry.instance().getStringLocalization("signpost.guiTooFar");
					if(out.equals("")){
						out = LanguageRegistry.instance().getStringLocalization("signpost.guiTooFar", "en_US");
					}
					out = out.replaceAll("<distance>", ""+(int)tile.toPos().distance(inf.pos)+1);
					out = out.replaceAll("<maxDist>", ""+ConfigHandler.maxDist);
					if(base2){
						std1 = out;
						col1 = Color.red.getRGB();
						go1 = false;
					}else{
						std2 = out;
						col2 = Color.red.getRGB();
						go2 = false;
					}
					
				}else if(connect.equals(Connection.WORLD)){

					String out = LanguageRegistry.instance().getStringLocalization("signpost.guiWorldDim");
					if(out.equals("")){
						out = LanguageRegistry.instance().getStringLocalization("signpost.guiWorldDim", "en_US");
					}
					if(base2){
						std1 = out;
						col1 = Color.red.getRGB();
						go1 = false;
					}else{
						std2 = out;
						col2 = Color.red.getRGB();
						go2 = false;
					}
					
				}else{
					if(base2){
						std1 = "";
						col1 = Color.red.getRGB();
						go1 = false;
					}else{
						std2 = "";
						col2 = Color.red.getRGB();
						go2 = false;
					}
				}
			}else{
				tf.textColor = Color.black.getRGB();
				if(base2){
					col1 = Color.white.getRGB();
					go1 = true;
				}else{
					col2 = Color.white.getRGB();
					go2 = true;
				}

				if(!(ConfigHandler.deactivateTeleportation||ConfigHandler.cost==null)){
					String out = LanguageRegistry.instance().getStringLocalization("signpost.guiPrev");
					if(out.equals("")){
						out = LanguageRegistry.instance().getStringLocalization("signpost.guiPrev", "en_US");
					}
					int distance = (int) tile.toPos().distance(inf.pos)+1;
					out = out.replaceAll("<distance>", ""+distance);
					out = out.replaceAll("<amount>", Integer.toString(PostHandler.getStackSize(tile.toPos(), inf.pos)));
					out = out.replaceAll("<itemName>", ConfigHandler.costName());
					if(base2){
						col1 = Color.white.getRGB();
						std1 = out;
					}else{
						col2 = Color.white.getRGB();
						std2 = out;
					}
				}
			}
		}
	}

	@Override
	public void onGuiClosed() {
		DoubleBaseInfo tilebases = tile.getBases();
		if(ConfigHandler.deactivateTeleportation||go2){
			tilebases.sign1.base = PostHandler.getForceWSbyName(base1InputBox.getText());
		}else{
			tilebases.sign1.base = null;
		}
		if(ConfigHandler.deactivateTeleportation||go1){
			tilebases.sign2.base = PostHandler.getForceWSbyName(base2InputBox.getText());
		}else{
			tilebases.sign2.base = null;
		}
		NetworkHandler.netWrap.sendToServer(new SendPostBasesMessage(tile, tilebases));
	}
}
