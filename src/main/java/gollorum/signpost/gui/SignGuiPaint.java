package gollorum.signpost.gui;

import java.io.IOException;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.Paintable;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;

public class SignGuiPaint extends GuiScreen {

	private GuiTextField nameInputBox;
	private Paintable paintable;
	private SuperPostPostTile tile;
	

	private boolean resetMouse;

	public SignGuiPaint(Paintable paintable, SuperPostPostTile tile) {
		this.paintable = paintable;
		this.tile = tile;
		initGui();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if(mc==null){
			mc = FMLClientHandler.instance().getClient();
		}
		if(nameInputBox==null){
			initGui();
		}
		drawDefaultBackground();
		if(nameInputBox.getText() == null || nameInputBox.getText().equals("null")){
			ResourceLocation loc = paintable==null ? tile.getPostPaint() : paintable.getTexture();
			String name;
			if(loc==null){
				name = "";
			}else{
				name = SuperPostPostTile.locToString(loc);
			}
			nameInputBox.setText(name);
		}
		try{
			nameInputBox.drawTextBox();
		}catch(Exception e){}

		if(resetMouse){
			resetMouse = false;
			org.lwjgl.input.Mouse.setGrabbed(false);
		}
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void initGui() {
		SPEventHandler.scheduleTask(new BoolRun(){
			@Override
			public boolean run(){
				if(fontRenderer==null){
					return false;
				}
				nameInputBox = new GuiTextField(0, fontRenderer, width/4, height/2 - 46, width/2, 20);
				nameInputBox.setMaxStringLength(100);
				ResourceLocation loc = paintable==null ? tile.getPostPaint() : paintable.getTexture();
				String name;
				if(loc==null){
					name = "";
				}else{
					name = SuperPostPostTile.locToString(loc);
				}
				nameInputBox.setText(name);
				return true;
			}
		});
		nameInputBox = new GuiTextField(0, this.fontRenderer, this.width/4, this.height/2 - 46, this.width/2, 20);
		nameInputBox.setMaxStringLength(100);
		if(mc==null){
			mc = FMLClientHandler.instance().getClient();
		}
		ResourceLocation loc = paintable==null ? tile.getPostPaint() : paintable.getTexture();
		String name;
		if(loc==null){
			name = "";
		}else{
			name = SuperPostPostTile.locToString(loc);
		}
		nameInputBox.setText(name);
		nameInputBox.setFocused(true);
		resetMouse = true;
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
		if(paintable!=null){
			paintable.setTexture(new ResourceLocation(nameInputBox.getText()));
		}else{
			tile.setPostPaint(new ResourceLocation(nameInputBox.getText()));
		}
		((SuperPostPost)tile.getBlockType()).sendPostBasesToAll(tile);
	}
}
