package gollorum.signpost.util.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class Lurchpaerchensauna<L1, L2> implements Map<L1, L2>, Cloneable, Serializable {

	private class Lurch{
		private L1 derEineLurch;
		private L2 derAndereLurch;
		private Lurch vorlurch = null;
		private Lurch nachlurch = null;
		private Lurch(L1 derEineLurch, L2 derAndereLurch){
			this.derEineLurch = derEineLurch;
			this.derAndereLurch = derAndereLurch;
		}
		private void putput(L1 derEineLurch, L2 derAndereLurch){
			nachlurch = new Lurch(derEineLurch, derAndereLurch);
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
	
	private Lurch alphalurch = new Lurch(null, null);
	private Lurch omegalurch = alphalurch;
	private int lurchmaechtigkeit = 0;
	
	public Lurchpaerchensauna(){}
	
	@Override
	public int size() {
		return lurchmaechtigkeit;
	}

	@Override
	public boolean isEmpty() {
		return lurchmaechtigkeit==0;
	}

	@Override
	public void clear() {
		Lurch jetztLurch = alphalurch;
		while(jetztLurch!=null){
			Lurch naechster = jetztLurch.nachlurch;
			jetztLurch.vorlurch = null;
			jetztLurch.nachlurch = null;
			jetztLurch.derEineLurch = null;
			jetztLurch.derAndereLurch = null;
			jetztLurch = naechster;
		}
		alphalurch = new Lurch(null, null);
		omegalurch = alphalurch;
		lurchmaechtigkeit = 0;
	}
	
	@Override
	public String toString(){
		switch(lurchmaechtigkeit){
		case 0: return "0: {}";
		default:
			String ret = lurchmaechtigkeit+": {";
			for(java.util.Map.Entry<L1, L2> now: this.entrySet()){
				ret = ret+now.getKey()+":"+now.getValue()+", ";
			}
			return ret.substring(0, ret.length()-2)+"}: "+lurchmaechtigkeit;
		}
	}

	@Override
	public boolean containsKey(Object schroedingersLurch) {
		Lurch jetztLurch = alphalurch;
		while((jetztLurch = jetztLurch.nachlurch)!=null){
			if(jetztLurch.derEineLurch.equals(schroedingersLurch)){
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsValue(Object schroedingersLurch) {
		Lurch jetztLurch = alphalurch;
		while((jetztLurch = jetztLurch.nachlurch)!=null){
			if(jetztLurch.derAndereLurch.equals(schroedingersLurch)){
				return true;
			}
		}
		return false;
	}
	
	public class Lurchpaerchen implements java.util.Map.Entry<L1, L2>{

		private Lurch paerchen;
		
		private Lurchpaerchen(Lurch paerchen){
			this.paerchen = paerchen;
		}
		
		@Override
		public L1 getKey() {
			return paerchen.derEineLurch;
		}

		@Override
		public L2 getValue() {
			return paerchen.derAndereLurch;
		}

		@Override
		public L2 setValue(L2 arg0) {
			return paerchen.derAndereLurch = arg0;
		}
		
	}

	@Override
	public Set<java.util.Map.Entry<L1, L2>> entrySet() {
		Lurchsauna<java.util.Map.Entry<L1, L2>> ret = new Lurchsauna<java.util.Map.Entry<L1, L2>>();
		Lurch jetztLurch = alphalurch;
		while((jetztLurch = jetztLurch.nachlurch)!=null){
			ret.add(new Lurchpaerchen(jetztLurch));
		}
		return ret;
	}

	@Override
	public L2 get(Object schroedingersLurch) {
		Lurch jetztLurch = alphalurch;
		while((jetztLurch = jetztLurch.nachlurch)!=null){
			if(jetztLurch.derEineLurch.equals(schroedingersLurch)){
				return jetztLurch.derAndereLurch;
			}
		}
		return null;
	}

	@Override
	public Set<L1> keySet() {
		Lurchsauna<L1> ret = new Lurchsauna<L1>();
		Lurch jetztLurch = alphalurch;
		while((jetztLurch = jetztLurch.nachlurch)!=null){
			ret.add(jetztLurch.derEineLurch);
		}
		return ret;
	}

	@Override
	public Collection<L2> values() {
		Lurchsauna<L2> ret = new Lurchsauna<L2>();
		Lurch jetztLurch = alphalurch;
		while((jetztLurch = jetztLurch.nachlurch)!=null){
			ret.add(jetztLurch.derAndereLurch);
		}
		return ret;
	}

	@Override
	public L2 put(L1 derEineLurch, L2 derAndereLurch) {
		if(derEineLurch==null){
			return null;
		}
		Lurch jetztLurch = alphalurch;
		while(jetztLurch.nachlurch!=null){
			if(jetztLurch.nachlurch.derEineLurch.equals(derEineLurch)){
				if(jetztLurch.nachlurch.derAndereLurch.equals(derAndereLurch)){
					return jetztLurch.nachlurch.derAndereLurch;
				}else{
					return jetztLurch.nachlurch.derAndereLurch = derAndereLurch;
				}
			}else{
				jetztLurch = jetztLurch.nachlurch;
			}
		}
		jetztLurch.putput(derEineLurch, derAndereLurch);
		return derAndereLurch;
	}

	@Override
	public void putAll(Map<? extends L1, ? extends L2> arg0) {
		for(java.util.Map.Entry<? extends L1, ? extends L2> now: arg0.entrySet()){
			put(now.getKey(), now.getValue());
		}
	}

	@Override
	public L2 remove(Object altlurch) {
		Lurch jetztLurch = alphalurch;
		while(jetztLurch.nachlurch!=null){
			if(jetztLurch.nachlurch.derEineLurch.equals(altlurch)){
				L2 ret = jetztLurch.nachlurch.derAndereLurch;
				jetztLurch.pufpuf();
				return ret;
			}else{
				jetztLurch = jetztLurch.nachlurch;
			}
		}
		return null;
	}

}
