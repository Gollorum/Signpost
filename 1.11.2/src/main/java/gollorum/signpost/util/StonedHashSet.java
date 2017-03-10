package gollorum.signpost.util;

import java.util.Collection;
import java.util.HashSet;

import gollorum.signpost.management.PostHandler;

public class StonedHashSet extends HashSet<BaseInfo>{

	public void merge(StonedHashSet other){
		StonedHashSet toDelete = new StonedHashSet();
		toDelete.addAll(this);
		for(BaseInfo otter: other){
			boolean found  = false;
			for(BaseInfo mai: this){
				if(otter.pos.equals(mai.pos)){
					mai.setAll(otter);
					toDelete.remove(mai);
					found = true;
					break;
				}
			}
			if(!found){
				this.add(otter);
			}
		}
		this.removeAll(toDelete);
	}

	public boolean nameTaken(String name){
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
	
	public boolean removeBaseInfo(MyBlockPos pos){
		return remove(getByPos(pos));
	}

	public BaseInfo getByPos(MyBlockPos pos){
		for(BaseInfo base: this){
			if(base.pos.equals(pos)){
				return base;
			}
		}
		return null;
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
			if(now.sameAs(toDelete)){
				toDelete = now;
				break;
			}
		}
		return super.remove(toDelete);
	}
}
