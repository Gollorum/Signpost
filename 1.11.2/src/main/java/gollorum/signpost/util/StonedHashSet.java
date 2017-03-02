package gollorum.signpost.util;

import java.util.Collection;
import java.util.HashSet;

import gollorum.signpost.management.PostHandler;

public class StonedHashSet extends HashSet<BaseInfo>{

	public boolean nameTaken(String name){
		for(BaseInfo now:this){
			if(now.name.equals(name)){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean add(BaseInfo now){
		boolean nowFound = false;
		boolean hasChanged = false;
		for(BaseInfo org: this){
			if(now.sameAs(org)){
				hasChanged = hasChanged | org.update(now);
				nowFound = true;
				break;
			}
		}
		if(!nowFound){
			hasChanged = hasChanged | super.add(now);
		}
		return hasChanged;
	}
	
	@Override
	public boolean addAll(Collection<? extends BaseInfo> c){
		boolean hasChanged = false;
		for(BaseInfo now: c){
			hasChanged = hasChanged | add(now);
		}
		return hasChanged;
	}
	
	public boolean addAll(HashSet<String> names){
		boolean ret = false;
		for(String nown: names){
			for(BaseInfo nowws: PostHandler.allWaystones){
				if(nowws.name.equals(nown)){
					add(nowws);
					ret = true;
				}
			}
		}
		return ret;
	}
	
}
