package gollorum.signpost.util;

import net.minecraft.init.Items;
import net.minecraft.item.Item;

public class BigBaseInfo {

	public Sign sign;
	public String[] description;
	
	public BigBaseInfo(){
		this(new Sign());
	}

	public BigBaseInfo(Sign sign){
		this.sign = sign;
		String[] description = {"Line 1", "Line 2", "Line 3", "Line 4"};
		this.description = description;
	}

	public BigBaseInfo(Sign sign, String[] description){
		this.sign = sign;
		this.description = description;
	}
}
