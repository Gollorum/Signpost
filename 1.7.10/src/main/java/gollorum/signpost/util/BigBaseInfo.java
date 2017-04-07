package gollorum.signpost.util;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class BigBaseInfo {

	public BaseInfo base;
	public int rotation = 90;
	public boolean flip;
	public OverlayType overlay = null;
	public boolean point;
	public String[] description;

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

	public BigBaseInfo(BaseInfo base, int datInt, boolean flip, OverlayType type, boolean point, String[] description) {
		this.base = base;
		this.rotation = datInt;
		this.flip = flip;
		this.overlay = type;
		this.point = point;
		this.description = description;
	}

	public BigBaseInfo(BaseInfo base, int datInt, boolean flip, OverlayType type, boolean point) {
		this.base = base;
		this.rotation = datInt;
		this.flip = flip;
		this.overlay = type;
		this.point = point;
		String[] description = {"Line 1", "Line 2", "Line 3", "Line 4"};
		this.description = description;
	}
	
}
