package gollorum.signpost.gui;

import java.awt.Color;
import java.io.IOException;

import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.management.ClientConfigStorage;
import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos.Connection;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;

public class SignGuiPost extends GuiScreen implements SignInput {

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
		initGui();
	}

	@Override
	public void initGui() {
		setupInputBox1();
		setupInputBox2();
		resetMouse = true;
		base1InputBox.setFocused(true);
	}
	
	private void setupInputBox1(){
		DoubleBaseInfo tilebases = tile.getBases();
		base1InputBox = new SignInputBox(this.fontRenderer, this.width / 2 - 68, this.height / 2 - 46, 137, this);
		base1InputBox.setText(tilebases.sign1.base==null?"":tilebases.sign1.base.toString());
		go1 = true;
	}
	
	private void setupInputBox2(){
		DoubleBaseInfo tilebases = tile.getBases();
		base2InputBox = new SignInputBox(this.fontRenderer, this.width / 2 - 68, this.height / 2 + 40, 137, this);
		base2InputBox.setText(tilebases.sign2.base==null?"":tilebases.sign2.base.toString());
		go2 = true;
	}
	
	@Override
    protected void mouseClicked(int x, int y, int bla) throws IOException{
		super.mouseClicked(x, y, bla);
		base1InputBox.mouseClicked(x, y, bla);
		base2InputBox.mouseClicked(x, y, bla);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		try{
			if(mc==null){
				mc = FMLClientHandler.instance().getClient();
			}
			if(base1InputBox==null){
				setupInputBox1();
			}
			if(base2InputBox==null){
				setupInputBox2();
			}
			drawDefaultBackground();
	
			base1InputBox.drawSignBox(fontRenderer);
			this.drawCenteredString(fontRenderer, std2, this.width/2, base1InputBox.y+base1InputBox.height+10, col2);
	
			base2InputBox.drawSignBox(fontRenderer);
			this.drawCenteredString(fontRenderer, std1, this.width/2, base2InputBox.y+base2InputBox.height+10, col1);
			
			if(resetMouse){
				resetMouse = false;
				org.lwjgl.input.Mouse.setGrabbed(false);
			}
		}catch(Exception e){}
	}

	@Override
	protected void keyTyped(char par1, int par2) throws IOException {
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
			if(ClientConfigStorage.INSTANCE.deactivateTeleportation()){
				return;
			}
			onTextChange(tf);
		}
	}

	@Override
	public void onGuiClosed() {
		DoubleBaseInfo tilebases = tile.getBases();
		if(ClientConfigStorage.INSTANCE.deactivateTeleportation()||go2){
			tilebases.sign1.base = PostHandler.getForceWSbyName(base1InputBox.getText());
		}else{
			tilebases.sign1.base = null;
		}
		if(ClientConfigStorage.INSTANCE.deactivateTeleportation()||go1){
			tilebases.sign2.base = PostHandler.getForceWSbyName(base2InputBox.getText());
		}else{
			tilebases.sign2.base = null;
		}
		NetworkHandler.netWrap.sendToServer(new SendPostBasesMessage(tile, tilebases));
	}

	@Override
	public void onTextChange(SignInputBox box) {
		boolean base2 = box==base2InputBox;
		BaseInfo inf = PostHandler.getWSbyName(box.getText());
		Connection connect = tile.toPos().canConnectTo(inf);
		if(inf==null||!connect.equals(Connection.VALID)){
			box.textColor = Color.red.getRGB();
			if(connect.equals(Connection.DIST)){

				String out = I18n.format("signpost.guiTooFar");
				out = out.replaceAll("<distance>", ""+(int)tile.toPos().distance(inf.teleportPosition)+1);
				out = out.replaceAll("<maxDist>", ""+ClientConfigStorage.INSTANCE.getMaxDist());
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

				String out = I18n.format("signpost.guiWorldDim");
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
			box.textColor = Color.black.getRGB();
			if(base2){
				col1 = Color.white.getRGB();
				go1 = true;
			}else{
				col2 = Color.white.getRGB();
				go2 = true;
			}

			if(!(ClientConfigStorage.INSTANCE.deactivateTeleportation()||ClientConfigStorage.INSTANCE.getCost()==null)){
				String out = I18n.format("signpost.guiPrev");
				int distance = (int) tile.toPos().distance(inf.teleportPosition)+1;
				out = out.replaceAll("<distance>", ""+distance);
				out = out.replaceAll("<amount>", Integer.toString(PostHandler.getStackSize(tile.toPos(), inf.teleportPosition)));
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
