package gollorum.signpost.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

import org.lwjgl.opengl.GL11;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.handlers.SendAllWaystoneNamesHandler;
import gollorum.signpost.util.BaseInfo;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;

public class SignInputBox extends Gui{
	
	private static final double pWidth = 25.0;
	private static final double pHeight = 6.0;
	private static final double verh = pWidth/pHeight;

	public int x;
	public int y;
	public int width;
	public int height;
	private boolean isFocused = false;
    private int cursorPosition;
    
    private ArrayList<String> possible = new ArrayList<String>();
    private int cycleTime = 1000; 
    private long lastSystemTime; 
    private int possibleIndex = 0;
    
    public int textColor = 0;
    private int cursorCycleTime = 500;
    public int drawXat;
    public double scc;

    public float[] boxColor = {1f, 1f, 1f, 1f};
    
    private static final ResourceLocation texture = new ResourceLocation("signpost:textures/gui/sign_gui.png");
	
	private FontRenderer fontRenderer;
	
	public String text = "";
	
	private final SignInput PARENT;
	
	public SignInputBox(FontRenderer p_i1032_1_, int x, int y, int width, SignInput parent) {
		this.fontRenderer = p_i1032_1_;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = (int) (width/verh);
		PARENT = parent;
		setText("");
		lastSystemTime = System.currentTimeMillis(); 
	}
	
	public void setFocused(boolean bool){
		isFocused = bool;
		cursorPosition = getText().length();
	}
	
	public boolean isFocused(){
		return isFocused;
	}
	
	public void drawSignBox(FontRenderer fontRend){
		this.fontRenderer = fontRend;
		GL11.glColor4f(boxColor[0], boxColor[1], boxColor[2], boxColor[3]);
		FMLClientHandler.instance().getClient().renderEngine.bindTexture(texture);
		drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, width, height, (float)pWidth*5.5f, (float)pHeight*5.5f);
		if(fontRend!=null){
			drawText();
		}
	}
	
	public void drawText(){
		String txt;
		if(drawCursor() && isFocused){
			txt = getText().substring(0, cursorPosition)+"|"+getText().substring(cursorPosition);
		}else{
			txt = getText();
		}
		double scale = this.width/pWidth;
		double x = this.x+(this.width)/2.0-scale;
		double y = this.y+(this.height)/2.0+scale/4;
		double sc2 = 100d/fontRenderer.getStringWidth(txt);
		if(sc2>=1){
			sc2 = 1;
		}
		scc = sc2;
		GL11.glPushMatrix();
		GL11.glScaled(sc2, sc2, 1);
		if(sc2==1.0){
			drawXat = (int) (x = (x-fontRenderer.getStringWidth(txt)/2.0));
		}else{
			drawXat = (int) (x-50);
			x = (x-50)/sc2;
		}
		fontRenderer.drawString(txt, (int) x, (int) ((y-fontRenderer.FONT_HEIGHT/2.0*sc2)/sc2), textColor);
		GL11.glPopMatrix();
		
		if(isFocused && possible.size()>0){
			if(cycleTimeHasPassed()){ 
				possibleIndex = possibleIndex+1;
			}
			lastSystemTime = System.currentTimeMillis(); 
			possibleIndex = possibleIndex%possible.size();
			fontRenderer.drawString(possible.get(possibleIndex), (int)(this.x+width+5), (int)(this.y+(scale*pHeight-fontRenderer.FONT_HEIGHT)/2.0), Color.WHITE.getRGB());
		}
	}

	private boolean drawCursor() {
		return (System.currentTimeMillis() / cursorCycleTime) % 2 == 0;
	}

	private boolean cycleTimeHasPassed() {
		return (System.currentTimeMillis() % cycleTime) <= System.currentTimeMillis() - lastSystemTime;
	}
	
	public void setText(String text){
		this.text = text;
		possible = new ArrayList<String>();
		for(String name: getAllPossibilities()){
			if(name.contains(getText()) || getText().equals("")){
				possible.add(name);
			}
		}
	}

	public static Collection<String> getAllPossibilities(){
		return PostHandler.getAllWaystoneNames();
	}

	
	public String getText(){
		return text;
	}

    public boolean textboxKeyTyped(char p_146201_1_, int p_146201_2_){
        if (!this.isFocused){
            return false;
        }else{
        	switch (p_146201_1_){
                case 1:
                    cursorPosition = getText().length();
                    return true;
                case 22:
                    this.writeText(GuiScreen.getClipboardString());
                    return true;
                default:
                    switch (p_146201_2_){
                    case 14:
                        if (GuiScreen.isCtrlKeyDown()){
                            this.deleteWords(-1);
                        }else{
                            this.deleteFromCursor(-1);
                        }
                        return true;
                        case 199:
                            cursorPosition = 0;
                            return true;
                        case 203:
                            if (GuiScreen.isCtrlKeyDown()){
                                this.setCursorPosition(this.getNthWordFromCursor(-1));
                            }else{
                                this.moveCursorBy(-1);
                            }
                            return true;
                        case 205:
                            if (GuiScreen.isCtrlKeyDown()){
                                this.setCursorPosition(this.getNthWordFromCursor(1));
                            }else{
                                this.moveCursorBy(1);
                            }
                            return true;
                        case 207:
                            cursorPosition = getText().length();
                            return true;
                        case 211:
                            if (GuiScreen.isCtrlKeyDown()){
                                this.deleteWords(1);
                            }
                            else{
                            	this.deleteFromCursor(1);
                            }
                            return true;
                        default:
                            if (ChatAllowedCharacters.isAllowedCharacter(p_146201_1_)){
                            	this.writeText(Character.toString(p_146201_1_));
                                return true;
                            }else{
                                return false;
                            }
                    }
            }
        }
    }

    public void writeText(String p_146191_1_){
        String s2 = ChatAllowedCharacters.filterAllowedCharacters(p_146191_1_);

        this.setText(getText().substring(0, cursorPosition)+s2+getText().substring(cursorPosition));
        this.moveCursorBy(s2.length());
    }

    public void moveCursorBy(int p_146182_1_){
        this.setCursorPosition(cursorPosition + p_146182_1_);
    }

    public void setCursorPosition(int p_146190_1_){
        this.cursorPosition = p_146190_1_;
        int j = this.getText().length();

        if (this.cursorPosition < 0){
            this.cursorPosition = 0;
        }else if (this.cursorPosition > j){
            this.cursorPosition = j;
        }
    }

    public void deleteFromCursor(int p_146175_1_){
        if (this.getText().length() != 0){
			boolean flag = p_146175_1_ < 0;
			int j = flag ? this.cursorPosition + p_146175_1_ : this.cursorPosition;
			int k = flag ? this.cursorPosition : this.cursorPosition + p_146175_1_;
			String s = "";
			
			if (j >= 0){
			    s = this.getText().substring(0, j);
			}
			
			if (k < this.getText().length()){
			    s = s + this.getText().substring(k);
			}
			
			this.setText(s);
			
			if (flag){
			    this.moveCursorBy(p_146175_1_);
			}
        }
    }

    public void deleteWords(int p_146177_1_){
        if (this.getText().length() != 0){
            this.deleteFromCursor(this.getNthWordFromCursor(p_146177_1_) - this.cursorPosition);
        }
    }

    public int getNthWordFromCursor(int p_146187_1_){
        return this.getNthWordFromPos(p_146187_1_, this.cursorPosition);
    }

    public int getNthWordFromPos(int p_146183_1_, int p_146183_2_){
        return this.func_146197_a(p_146183_1_, this.cursorPosition, true);
    }

    public int func_146197_a(int p_146197_1_, int p_146197_2_, boolean p_146197_3_){
        int k = p_146197_2_;
        boolean flag1 = p_146197_1_ < 0;
        int l = Math.abs(p_146197_1_);

        for (int i1 = 0; i1 < l; ++i1)
        {
            if (flag1)
            {
                while (p_146197_3_ && k > 0 && this.getText().charAt(k - 1) == 32)
                {
                    --k;
                }

                while (k > 0 && this.getText().charAt(k - 1) != 32)
                {
                    --k;
                }
            }
            else
            {
                int j1 = this.getText().length();
                k = this.getText().indexOf(32, k);

                if (k == -1)
                {
                    k = j1;
                }
                else
                {
                    while (p_146197_3_ && k < j1 && this.getText().charAt(k) == 32)
                    {
                        ++k;
                    }
                }
            }
        }

        return k;
    }

    public void mouseClicked(int x, int y, int p_146192_3_){
        isFocused = (x >= this.x &&
        			 x < this.x + this.width &&
        			 y >= this.y &&
        			 y < this.y + this.height);

		double scale = this.width/pWidth;
		try{
	        if(x >= (this.x+width+5) &&
	           x < (this.x+width+5+fontRenderer.getStringWidth(possible.get(possibleIndex))) &&
	           y >= (this.y+(scale*pHeight-fontRenderer.FONT_HEIGHT)/2.0) &&
	           y < (this.y+(scale*pHeight-fontRenderer.FONT_HEIGHT)/2.0)+fontRenderer.FONT_HEIGHT){
	        	this.setText(possible.get(possibleIndex));
	        	PARENT.onTextChange(this);
	        	isFocused = true;
	        }
		}catch(Exception e){}
		
        if (this.isFocused && p_146192_3_ == 0){
            int l = x - drawXat;
            this.setCursorPosition(correctTrim((int) (l/scc)).length());
        }
    }
    
    public String correctTrim(int width){
    	int l = this.fontRenderer.trimStringToWidth(getText(), width).length();
    	if(getText().length() == l){
    		return getText();
    	}
    	int l1 = this.fontRenderer.getStringWidth(getText().substring(0, l));
    	int l2 = this.fontRenderer.getStringWidth(getText().substring(0, l+1));
    	if(width-l1<l2-width){
    		return getText().substring(0, l);
    	}else{
    		return getText().substring(0, l+1);
    	}
    }

	public void setTextColor(int rgb) {
		textColor = rgb;
	}

}
