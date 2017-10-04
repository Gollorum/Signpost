package gollorum.signpost.util;

import java.util.Collection;
import java.util.HashSet;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.collections.Lurchsauna;

public class StonedHashSet extends Lurchsauna<BaseInfo>{

	public BaseInfo getByPos(MyBlockPos pos){
		for(BaseInfo now: this){
			if(now.blockPos.equals(pos)){
				return now;
			}
		}
		return null;
	}

	public BaseInfo getByName(String waystoneName) {
		for(BaseInfo now: this){
			if(now.name.equals(waystoneName)){
				return now;
			}
		}
		return null;
	}
	
	public boolean nameTaken(String name){
		if(name==null){
			return false;
		}
		for(BaseInfo now:this){
			if(name.equals(now.name)){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean add(BaseInfo now){
		boolean nowFound = false;
		for(BaseInfo org: this){
			if(org.update(now)){
				nowFound = true;
				break;
			}
		}
		if(!nowFound){
			nowFound = super.add(now);
		}
		return nowFound;
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
	
	public boolean removeByPos(MyBlockPos pos){
		BaseInfo toDelete = getByPos(pos);
		if(toDelete==null){
			return true;
		}else{
			return super.remove(toDelete);
		}
	}
	
	@Override
	public boolean remove(Object obj) {
		if(!(obj instanceof BaseInfo)){
			return false;
		}
		BaseInfo toDelete = (BaseInfo)obj;
		for(BaseInfo now: this){
			if(now.equals(toDelete)){
				toDelete = now;
				break;
			}
		}
		return super.remove(toDelete);
	}
}
