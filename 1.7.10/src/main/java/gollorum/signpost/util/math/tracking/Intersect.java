package gollorum.signpost.util.math.tracking;

public class Intersect {
	
	public boolean exists;
	public DDDVector pos;
	
	public Intersect(boolean exists, DDDVector pos){
		this.exists = exists;
		this.pos = pos;
	}
	
	public String toString(){
		if(exists){
			return pos.toString();
		}else{
			return "Intersect does not exist";
		}
	}
	
}