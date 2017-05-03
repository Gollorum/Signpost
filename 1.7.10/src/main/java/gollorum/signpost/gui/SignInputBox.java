package gollorum.signpost.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

public class SignInputBox extends Gui{
	
	private static final double pWidth = 25.0;
	private static final double pHeight = 6.0;
	private static final double verh = pWidth/pHeight;

	private int x;
	private int y;
	private int width;
	private int height;
	
	private FontRenderer fontRend;
	
	public String text = "";
	
	public SignInputBox(FontRenderer p_i1032_1_, int x, int y, int width) {
		this.fontRend = p_i1032_1_;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = (int) (width/verh);
//		super(p_i1032_1_, x, y, width, (int) (width/verh));
	}
	
	public void drawSignBox(){
		drawTexturedModalRect(this.x, this.y, 0, 0, width, height);
		drawText();
	}
	
	@Override  
	public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height){
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV((double)(x), 		(double)(y+height), (double)this.zLevel, 0.0, 1.0);
        tessellator.addVertexWithUV((double)(x+width), 	(double)(y+height), (double)this.zLevel, 1.0, 1.0);
        tessellator.addVertexWithUV((double)(x+width), 	(double)(y), 		(double)this.zLevel, 1.0, 0.0);
        tessellator.addVertexWithUV((double)(x),		(double)(y), 		(double)this.zLevel, 0.0, 0.0);
        tessellator.draw();
    }
	
	public void drawText(){
		double scale = this.width/pWidth;
		double x = this.x+scale+(this.height-fontRend.FONT_HEIGHT)/2.0;
		double y = this.y+scale+(this.width-fontRend.getStringWidth(getText()))/2.0;
		fontRend.drawString(getText(), (int) x, (int) y, 14737632);
	}
	
	public void setText(String text){
		this.text = text;
	}
	
	public String getText(){
		return text;
	}
}
