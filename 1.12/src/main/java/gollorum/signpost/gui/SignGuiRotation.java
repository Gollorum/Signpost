package gollorum.signpost.gui;

import java.awt.Color;
import java.io.IOException;

import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.math.tracking.DDDVector;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.FMLClientHandler;

public class SignGuiRotation extends GuiScreen {

	private Sign sign;
	private SuperPostPostTile tile;

	private GuiTextField degreesInputBox;

	private GuiTextField xInputBox;
	private GuiTextField zInputBox;
	
	public SignGuiRotation(Sign sign, SuperPostPostTile tile){
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
		if(degreesInputBox==null || xInputBox==null || zInputBox==null){
			initGui();
		}
		drawDefaultBackground();
		if(degreesInputBox.getText() == null || degreesInputBox.getText().equals("null")){
			degreesInputBox.setText(""+sign.rotation);
		}
		String str = "Angle (degrees): ";
		int stringwidth = fontRenderer.getStringWidth(str);
		fontRenderer.drawString(str, degreesInputBox.x-stringwidth, degreesInputBox.y+(degreesInputBox.height-fontRenderer.FONT_HEIGHT)/2, Color.white.getRGB());

		str = "Block position to point to:   X= ";
		stringwidth = fontRenderer.getStringWidth(str);
		fontRenderer.drawString(str, xInputBox.x-stringwidth, xInputBox.y+(xInputBox.height-fontRenderer.FONT_HEIGHT)/2, Color.white.getRGB());

		str = "Z= ";
		stringwidth = fontRenderer.getStringWidth(str);
		fontRenderer.drawString(str, zInputBox.x-stringwidth, zInputBox.y+(xInputBox.height-fontRenderer.FONT_HEIGHT)/2, Color.white.getRGB());
		
		degreesInputBox.drawTextBox();
		xInputBox.drawTextBox();
		zInputBox.drawTextBox();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void initGui() {
		degreesInputBox = new GuiTextField(0, this.fontRenderer, 140, this.height/2 - 46, this.width/2, 20);
		degreesInputBox.setMaxStringLength(100);
		xInputBox = new GuiTextField(1, this.fontRenderer, this.width/2-20, this.height/2, 60, 20);
		xInputBox.setMaxStringLength(100);
		zInputBox = new GuiTextField(2, this.fontRenderer, this.width/2+80, this.height/2, 60, 20);
		zInputBox.setMaxStringLength(100);
		if(mc==null){
			mc = FMLClientHandler.instance().getClient();
		}
		if(sign==null){
			this.mc.displayGuiScreen(null);
			return;
		}
		degreesInputBox.setText(""+sign.rotation);
		degreesInputBox.setFocused(true);
		xInputBox.setFocused(false);
		zInputBox.setFocused(false);
	}

	@Override
	protected void keyTyped(char par1, int par2) throws IOException {
		if(par1==13){
			if(degreesInputBox.isFocused()){
				this.mc.displayGuiScreen(null);
			}else if(xInputBox.isFocused()){
				xInputBox.setFocused(false);
				zInputBox.setFocused(true);
			}else if(zInputBox.isFocused()){
				try{
					double x = Double.parseDouble(xInputBox.getText());
					double z = Double.parseDouble(zInputBox.getText());
					double dx = x-tile.getPos().getX();
					double dz = z-tile.getPos().getZ();
					double degree = Math.toDegrees(DDDVector.genAngle(dx, dz)+Math.toRadians(-90+(dx<0&&dz>0?180:0)));
					degreesInputBox.setText(""+degree);
					this.mc.displayGuiScreen(null);
				}catch(Exception e2){}
			}
			return;
		}else if(par1==9){
			if(degreesInputBox.isFocused()){
				degreesInputBox.setFocused(false);
				xInputBox.setFocused(true);
			}else if(xInputBox.isFocused()){
				xInputBox.setFocused(false);
				zInputBox.setFocused(true);
			}else{
				zInputBox.setFocused(false);
				degreesInputBox.setFocused(true);
			}
			return;
		}
		super.keyTyped(par1, par2);
		if(degreesInputBox.isFocused()){
			String txt = degreesInputBox.getText();
			this.degreesInputBox.textboxKeyTyped(par1, par2);
			try{
				Double.parseDouble(degreesInputBox.getText());
			}catch(Exception e){
				try{
					Double.parseDouble(degreesInputBox.getText()+"0");
				}catch(Exception e2){
					degreesInputBox.setText(txt);
					return;
				}
			}
			return;
		}
		GuiTextField field;
		if(xInputBox.isFocused()){
			field = xInputBox;
		}else if(zInputBox.isFocused()){
			field = zInputBox;
		}else{
			return;
		}
		String txt = field.getText();
		field.textboxKeyTyped(par1, par2);
		try{
			Double.parseDouble(field.getText());
			try{
				double x = Double.parseDouble(xInputBox.getText());
				double z = Double.parseDouble(zInputBox.getText());
				double dx = x-tile.getPos().getX();
				double dz = z-tile.getPos().getZ();
				double degree = Math.toDegrees(DDDVector.genAngle(dx, dz)+Math.toRadians(-90+(dx<0&&dz>0?180:0)));
				degreesInputBox.setText(""+degree);
			}catch(Exception e2){}
		}catch(Exception e){
			try{
				Double.parseDouble(field.getText()+"0");
			}catch(Exception e2){
				field.setText(txt);
				return;
			}
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		this.degreesInputBox.updateCursorCounter();
		this.xInputBox.updateCursorCounter();
		this.zInputBox.updateCursorCounter();
	}

	@Override
	protected void mouseClicked(int x, int y, int btn) throws IOException {
		super.mouseClicked(x, y, btn);
		this.degreesInputBox.mouseClicked(x, y, btn);
		this.xInputBox.mouseClicked(x, y, btn);
		this.zInputBox.mouseClicked(x, y, btn);
	}

	@Override
	public void onGuiClosed() {
		try{
			sign.rotation = (int)Double.parseDouble(degreesInputBox.getText());
			((SuperPostPost)tile.getBlockType()).sendPostBasesToServer(tile);
		}catch(Exception e){}
		super.onGuiClosed();
	}
}
