package gollorum.signpost.util;

import java.util.HashSet;

public class MyBlockPosSet extends HashSet<MyBlockPos> {

	@Override
	public boolean add(MyBlockPos str) {
		for (MyBlockPos now : this) {
			if (now.equals(str)){
				return false;
			}
		}
		super.add(str);
		return true;
	}

	@Override
	public boolean contains(Object str) {
		for (MyBlockPos now : this) {
			if (str.equals(now)) {
				return true;
			}
		}
		return false;
	}

}