package gollorum.signpost.util.math.tracking;

import gollorum.signpost.util.math.MatrixD;

public class Rectangle {
	
	DDDVector offset;
	DDDVector rotation;
	DDDVector edge1;
	DDDVector edge2;
	
	public Rectangle(DDDVector origin, DDDVector edge1, DDDVector edge2){
		offset = origin.copy();

		this.edge1 = edge1.copy();
		this.edge2 = edge2.copy();
	}

	public Intersect traceLine(DDDVector start, DDDVector end, boolean unlimited){
		DDDVector v1 = edge1;
		DDDVector v2 = edge2;
		DDDVector v3 = end.substract(start);
		DDDVector gme = start.substract(offset);
		
		double[][] m = {{v1.x, v2.x, -v3.x, gme.x},
						{v1.y, v2.y, -v3.y, gme.y},
						{v1.z, v2.z, -v3.z, gme.z}};

		MatrixD matrix = new MatrixD(m);
		matrix.gaussAlgorithm();
		
		if(!matrix.check()){
			return new Intersect(false, null);
		}else{
			double r = matrix.get(0, 3);
			double s = matrix.get(1, 3);
			double t = matrix.get(2, 3);
			DDDVector pos = start.add(v3.mult(t));
			return new Intersect(test01(r) && test01(s) && (unlimited||test01(t)), pos);
		}
	}
	private static boolean test01(double var){
		return -0.00000000001<var&&var<1.00000000001;
	}	
}