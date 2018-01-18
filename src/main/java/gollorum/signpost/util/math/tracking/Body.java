package gollorum.signpost.util.math.tracking;

import java.util.ArrayList;

public class Body extends ArrayList<Rectangle>{

	public Body(int i) {super(i);}

	public Intersect traceLine(DDDVector start, DDDVector end, boolean unlimited){
		if(this.size()==0){
			return new Intersect(false, null);
		}
		Intersect nearest = new Intersect(false, null);
		double d = Double.MAX_VALUE;
		for(Rectangle now: this){
			Intersect inter = now.traceLine(start, end, unlimited);
			double dn;
			if(inter.exists && (dn = inter.pos.sqrDistance(start))<d){
				nearest = inter;
				d = dn;
			}
		}
		return nearest;
	}

}
