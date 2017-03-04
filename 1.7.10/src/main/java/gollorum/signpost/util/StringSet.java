package gollorum.signpost.util;

import java.util.HashSet;

public class StringSet extends HashSet<String> {

	@Override
	public boolean add(String str) {
		for (String now : this) {
			if (now.equals(str)){
				return false;
			}
		}
		super.add(str);
		return true;
	}

	@Override
	public boolean contains(Object str) {
		for (String now : this) {
			if (str.equals(now)) {
				return true;
			}
		}
		return false;
	}

}
