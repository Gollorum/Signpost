package gollorum.signpost.util.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class Lurchsauna<L> implements Set<L>, Cloneable, Serializable {

	private class Lurch{
		private L derLurch;
		private Lurch vorlurch = null;
		private Lurch nachlurch = null;
		private Lurch(L derLurch){
			this.derLurch = derLurch;
		}
		private void putput(L frischfleisch){
			nachlurch = new Lurch(frischfleisch);
			nachlurch.vorlurch = this;
			lurchmaechtigkeit++;
			if(this==omegalurch){
				omegalurch = nachlurch;
			}
		}
		private void pufpuf(){
			if(nachlurch == omegalurch){
				omegalurch = this;
				nachlurch.vorlurch = null;
				nachlurch = null;
			}else{
				Lurch nl = nachlurch.nachlurch;
				nachlurch.vorlurch = null;
				nachlurch.nachlurch = null;
				nl.vorlurch = this;
				nachlurch = nl;
			}
			lurchmaechtigkeit--;
		}
	}
	
	private class Lurchaufseher implements Iterator<L>{
		private Lurch arbeiterlurch;
		private Lurchaufseher(){
			arbeiterlurch = alphalurch;
		}
		@Override
		public boolean hasNext() {
			return arbeiterlurch.nachlurch!=null;
		}
		@Override
		public L next() {
			return (arbeiterlurch=arbeiterlurch.nachlurch).derLurch;
		}
	}
	
	private Lurch alphalurch = new Lurch(null);
	private Lurch omegalurch = alphalurch;
	private int lurchmaechtigkeit = 0;
	
	public Lurchsauna(){}
	
	public Lurchsauna(L[] lurchbande) {
		for(L now: lurchbande){
			add(now);
		}
	}
	
	public Lurchsauna(Collection<? extends L> lurchbande) {
		for(L now: lurchbande){
			add(now);
		}
	}
	
	public L get(int index){
		if(index<0 || index>=lurchmaechtigkeit){
			return null;
		}
		Lurch jetztLurch = alphalurch;
		while(index>0){
			jetztLurch = jetztLurch.nachlurch;
			index--;
		}
		return jetztLurch.nachlurch.derLurch;
	}

	@Override
	public int size() {
		return lurchmaechtigkeit;
	}

	@Override
	public boolean isEmpty() {
		return lurchmaechtigkeit==0;
	}

	@Override
	public boolean contains(Object schroedingersLurch) {
		Lurch jetztLurch = alphalurch;
		while((jetztLurch = jetztLurch.nachlurch)!=null){
			if(jetztLurch.derLurch.equals(schroedingersLurch)){
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<L> iterator() {
		return new Lurchaufseher();
	}

	@Override
	public Object[] toArray() {
		Object[] ret = new Object[lurchmaechtigkeit];
		Lurch jetztLurch = alphalurch;
		for(int i=0; i<lurchmaechtigkeit; i++){
			ret[i] = (jetztLurch = jetztLurch.nachlurch).derLurch;
		}
		return ret;
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		return arg0;
	}

	@Override
	public boolean add(L frischfleisch) {
		if(frischfleisch==null){
			return false;
		}
		Lurch jetztLurch = alphalurch;
		while(jetztLurch.nachlurch!=null){
			if(jetztLurch.nachlurch.derLurch.equals(frischfleisch)){
				return false;
			}else{
				jetztLurch = jetztLurch.nachlurch;
			}
		}
		jetztLurch.putput(frischfleisch);
		return true;
	}

	@Override
	public boolean remove(Object altlurch) {
		Lurch jetztLurch = alphalurch;
		while(jetztLurch.nachlurch!=null){
			if(jetztLurch.nachlurch.derLurch.equals(altlurch)){
				jetztLurch.pufpuf();
				return true;
			}else{
				jetztLurch = jetztLurch.nachlurch;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> lurchbande) {
		ArrayList<Object> uebrig = new ArrayList<Object>(lurchbande);
		uebrig.removeAll(this);
		return uebrig.isEmpty();
	}

	@Override
	public boolean addAll(Collection<? extends L> lurchbande) {
		Lurchsauna<L> uebrig = new Lurchsauna<L>(lurchbande);
		uebrig.removeAll(this);
		for(L jetztLurch: uebrig){
			if(jetztLurch!=null){
				omegalurch.putput(jetztLurch);
			}
		}
		return !uebrig.isEmpty();
	}

	@Override
	public boolean removeAll(Collection<?> lurchbande) {
		boolean ret = false;
		for(Object altlurch: lurchbande){
			ret = ret|remove(altlurch);
		}
		return ret;
	}

	@Override
	public boolean retainAll(Collection<?> lurchbande) {
		Lurchsauna<L> toDelete = new Lurchsauna<L>();
		toDelete.addAll(this);
		toDelete.removeAll(lurchbande);
		return removeAll(toDelete);
	}

	@Override
	public void clear() {
		Lurch jetztLurch = alphalurch;
		while(jetztLurch!=null){
			Lurch naechster = jetztLurch.nachlurch;
			jetztLurch.vorlurch = null;
			jetztLurch.nachlurch = null;
			jetztLurch.derLurch = null;
			jetztLurch = naechster;
		}
		alphalurch = new Lurch(null);
		omegalurch = alphalurch;
		lurchmaechtigkeit = 0;
	}
	
	@Override
	public String toString(){
		switch(lurchmaechtigkeit){
		case 0: return "0: {}";
		default:
			String ret = lurchmaechtigkeit+": {";
			for(L now:this){
				ret = ret+now+", ";
			}
			return ret.substring(0, ret.length()-2)+"}: "+lurchmaechtigkeit;
		}
	}

	public <T> Collection<T> select(Function<L, T> consumer){
		HashSet<T> ret = new HashSet<T>(size());
		for(L now : this) {
			ret.add(consumer.apply(now));
		}
		return ret;
	}

}
