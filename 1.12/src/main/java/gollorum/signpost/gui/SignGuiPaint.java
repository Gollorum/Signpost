package gollorum.signpost.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.Signpost;
import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.ResourceBrowser;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.collections.Lurchsauna;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.client.FMLClientHandler;

public class SignGuiPaint extends GuiScreen {

	private GuiTextField nameInputBox;
	private Sign sign;
	private SuperPostPostTile tile;
	
	private Lurchsauna<String> possibilities = new Lurchsauna<String>();
    private int possibleCount = 0;
    private int possibleIndex = 0;

	private boolean resetMouse;

	public SignGuiPaint(Sign sign, SuperPostPostTile tile) {
		this.sign = sign;
		this.tile = tile;
		System.out.println("Construct SignGuiPaint");
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
			ResourceLocation loc = sign==null ? tile.getPostPaint() : sign.paint;
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

		if(possibilities.size()>0){
			possibleCount = (possibleCount+1)%150;
			if(possibleCount == 149){
				possibleIndex = possibleIndex+1;
			}
			possibleIndex = possibleIndex%possibilities.size();
			String str = possibilities.get(possibleIndex);
			fontRenderer.drawString(str, 
					(int)(nameInputBox.x+(nameInputBox.width-fontRenderer.getStringWidth(str))/2.0), 
					(int)(nameInputBox.y+nameInputBox.height+5), 
					Color.WHITE.getRGB());
		}

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
				ResourceLocation loc = sign==null ? tile.getPostPaint() : sign.paint;
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
		ResourceLocation loc = sign==null ? tile.getPostPaint() : sign.paint;
		String name;
		if(loc==null){
			name = "";
		}else{
			name = SuperPostPostTile.locToString(loc);
		}
		nameInputBox.setText(name);
		nameInputBox.setFocused(true);
		Lurchsauna<String> neuPossibels = new Lurchsauna<String>();
		System.out.println("GANZ NEU POSIBELSSS");
		Logger.getLogger(Signpost.MODID).log(Level.ALL, "1Get Files?");
		Logger.getLogger(Signpost.MODID).log(Level.SEVERE, "2Get Files?");
		Logger.getLogger(Signpost.MODID).log(Level.CONFIG, "3Get Files?");
		Logger.getLogger(Signpost.MODID).log(Level.FINE, "4Get Files?");
		Logger.getLogger(Signpost.MODID).log(Level.FINER, "5Get Files?");
		Logger.getLogger(Signpost.MODID).log(Level.INFO, "6Get Files?");
		Logger.getLogger(Signpost.MODID).log(Level.OFF, "7Get Files?");
		Logger.getLogger(Signpost.MODID).log(Level.WARNING, "8Get Files?");
		mc.player.sendMessage(new TextComponentString("INIT DU PENIAS"));
		for(String now: ResourceBrowser.reloadAllPNGs(mc)){
			if(nameInputBox.getText()==null || nameInputBox.getText().equals("") || now.contains(nameInputBox.getText())){
				neuPossibels.add(now);
			}
		}
		possibilities = neuPossibels;
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
		Lurchsauna<String> neuPossibels = new Lurchsauna<String>();
		System.out.println("NE-U POSIBELSSS");
		for(String now: ResourceBrowser.getAllPNGs(mc)){
			if(nameInputBox.getText()==null || nameInputBox.getText().equals("") || now.contains(nameInputBox.getText())){
				neuPossibels.add(now);
			}
		}
		possibilities = neuPossibels;
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
		String str = possibilities.get(possibleIndex);
		if(x>=nameInputBox.x+(nameInputBox.width-fontRenderer.getStringWidth(str))/2.0
				&& x<=nameInputBox.x+(nameInputBox.width+fontRenderer.getStringWidth(str))/2.0
				&& y>=nameInputBox.y+nameInputBox.height+5
				&& y<=nameInputBox.y+nameInputBox.height*2+5){
			nameInputBox.setText(str);
		}
		possibilities = new Lurchsauna<String>(new String[]{str});
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		if(sign!=null){
			sign.paint = new ResourceLocation(nameInputBox.getText());
		}else{
			tile.setPostPaint(new ResourceLocation(nameInputBox.getText()));
		}
		((SuperPostPost)tile.getBlockType()).sendPostBasesToAll(tile);
	}
}
