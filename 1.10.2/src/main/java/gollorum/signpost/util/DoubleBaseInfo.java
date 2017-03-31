package gollorum.signpost.util;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class DoubleBaseInfo {

	public BaseInfo base1;
	public BaseInfo base2;

	public int rotation1 = 90;
	public int rotation2 = 90;

	public boolean flip1;
	public boolean flip2;

	public OverlayType overlay1 = null;
	public OverlayType overlay2 = null;
	
	public boolean point1;
	public boolean point2;

	public static enum OverlayType{
		GRAS(	"grass",	Items.WHEAT_SEEDS),
		SNOW(	"snow",		Items.SNOWBALL);
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

	public DoubleBaseInfo(BaseInfo base1, BaseInfo base2, int int1, int int2, boolean flip1, boolean flip2, OverlayType type1, OverlayType type2, boolean point1, boolean point2) {
		this.base1 = base1;
		this.base2 = base2;
		this.rotation1 = int1;
		this.rotation2 = int2;
		this.flip1 = flip1;
		this.flip2 = flip2;
		this.overlay1 = type1;
		this.overlay2 = type2;
		this.point1 = point1;
		this.point2 = point2;
	}
	
}
