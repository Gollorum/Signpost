package gollorum.signpost.util;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class BigBaseInfo {

	public BaseInfo base;
	public int rotation = 90;
	public boolean flip;
	public OverlayType overlay = null;
	public boolean point;
	public String[] description;
	public ResourceLocation signPaint = null;
	
	public static enum OverlayType{
		GRAS(	"grass",	Items.wheat_seeds),
		SNOW(	"snow",		Items.snowball);
		public String texture;
		public Item item;
		OverlayType(String texture, Item item){
			this.texture = texture;
			this.item = item;
		}
		public static OverlayType get(String arg){
			try{
				return valueOf(arg);
			}catch(IllegalArgumentException e){
				return null;
			}
		}
	}

	public BigBaseInfo(BaseInfo base, int datInt, boolean flip, OverlayType type, boolean point, String[] description, ResourceLocation signPaint) {
		this.base = base;
		this.rotation = datInt;
		this.flip = flip;
		this.overlay = type;
		this.point = point;
		this.description = description;
		this.signPaint = signPaint;
		this.signPaint = signPaint;
	}

	public BigBaseInfo(BaseInfo base, int datInt, boolean flip, OverlayType type, boolean point, ResourceLocation signPaint) {
		this.base = base;
		this.rotation = datInt;
		this.flip = flip;
		this.overlay = type;
		this.point = point;
		String[] description = {"Line 1", "Line 2", "Line 3", "Line 4"};
		this.description = description;
		this.signPaint = signPaint;
	}
	
}
