package gollorum.signpost.util;

import gollorum.signpost.management.PostHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

public class StonedHashSet extends CopyOnWriteArraySet<BaseInfo> {

	public BaseInfo getByPos(MyBlockPos pos){
		for(BaseInfo now: this){
			if(now.blockPosition.equals(pos)){
				return now;
			}
		}
		return null;
	}

	public BaseInfo getByName(String waystoneName) {
		for(BaseInfo now: this){
			if(now.getName().equals(waystoneName)){
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
			if(name.equals(now.getName())){
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
			for(BaseInfo nowws: PostHandler.getAllWaystones()){
				if(nowws.getName().equals(nown)){
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

	public Collection<MyBlockPos> positions() {
		Set<MyBlockPos> positions = new HashSet<>(size());
		for (BaseInfo now : this) {
			positions.add(now.blockPosition);
		}
		return positions;
	}

	public <T> Collection<T> select(Function<BaseInfo, T> mapping){
		HashSet<T> ret = new HashSet<T>();
		for(BaseInfo info: this) {
			ret.add(mapping.apply(info));
		}
		return ret;
	}
}
