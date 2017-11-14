package gollorum.signpost.util.math.tracking;

public class DDDVector {

	public double x, y, z;
	
	public DDDVector(double x, double y, double z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public DDDVector(DDDVector other){
		this(other.x, other.y, other.z);
	}
	
	public DDDVector add(DDDVector other){
		return new DDDVector(this.x+other.x, this.y+other.y, this.z+other.z);
	}
	
	public DDDVector substract(DDDVector other){
		return new DDDVector(this.x-other.x, this.y-other.y, this.z-other.z);
	}
	
	public DDDVector mult(double fac){
		return new DDDVector(x*fac, y*fac, z*fac);
	}
	
	public DDDVector neg(){
		return new DDDVector(-x, -y, -z);
	}
	
	public double sqrDistance(DDDVector other){
		double dx = this.x-other.x;
		double dy = this.y-other.y;
		double dz = this.z-other.z;
		return dx*dx+dy*dy+dz*dz;
	}
	
	public double distance(DDDVector other){
		return Math.sqrt(sqrDistance(other));
	}
	
	public double getlength(){
		return Math.sqrt(x*x+y*y+z*z);
	}

	public DDDVector setLength(double newLength) {
		double mult = newLength/getlength();
		return mult(mult);
	}

	public DDDVector xVec(){
		return new DDDVector(x, 0, 0);
	}

	public DDDVector yVec(){
		return new DDDVector(0, y, 0);
	}

	public DDDVector zVec(){
		return new DDDVector(0, 0, z);
	}
	
	/**
	 * Rotates around x axis
	 * @param angle The angle in radians
	 * @return The rotated Vector
	 */
	public DDDVector rotX(double angle){
		double a = angle+genAngle(z, y);
		double d = Math.sqrt(y*y+z*z);
		double y = d*Math.cos(a);
		double z = d*Math.sin(a);
		return new DDDVector(x, y, z);
	}

	/**
	 * Rotates around y axis
	 * @param angle The angle in radians
	 * @return The rotated Vector
	 */
	public DDDVector rotY(double angle){
		double a = angle+genAngle(x, z);
		double d = Math.sqrt(x*x+z*z);
		double z = d*Math.cos(a);
		double x = d*Math.sin(a);
		return new DDDVector(x, y, z);
	}

	/**
	 * Rotates around z axis
	 * @param angle The angle in radians
	 * @return The rotated Vector
	 */
	public DDDVector rotZ(double angle){
		double a = angle+genAngle(y, x);
		double d = Math.sqrt(x*x+y*y);
		double x = d*Math.cos(a);
		double y = d*Math.sin(a);
		return new DDDVector(x, y, z);
	}
	
	public static double genAngle(double axis1, double axis2){
		if(axis2==0){
			if(axis1<0){
				return Math.PI*3/2;
			}else{
				return Math.PI/2;
			}
		}
		double ret = Math.atan(axis1/Math.abs(axis2));
		return axis2<0?Math.PI-ret:ret<0?Math.PI+ret:ret;
	}
	
	public double rotAroundXToZ0(){
		double rot = -Math.atan(z/y);
		y = Math.sqrt(z*z+y*y);
		z = 0;
		return rot;
	}

	public double rotAroundZToX0(){
		double rot = Math.PI/2-Math.atan(y/x);
		y = Math.sqrt(x*x+y*y);
		x = 0;
		return rot;
	}

	public double rotAroundYToZ0(){
		double rot = Math.PI/2-Math.atan(x/z);
		x = Math.sqrt(x*x+z*z);
		z = 0;
		return rot;
	}
	
	public DDDVector unrot(DDDVector rot){
		return rotate(rot.mult(-1));
	}
	
	public DDDVector rotate(DDDVector rot){
		DDDVector ret = new DDDVector(this);

		ret = ret.rotX(rot.x);
		ret = ret.rotY(rot.y);
		ret = ret.rotZ(rot.z);
		
		return ret;		
	}
	
	@Override
	public String toString(){
		return x+"|"+y+"|"+z;
	}

	public double[] toArray(){
		double[] ret = {x, y, z};
		return ret;
	}

	public DDDVector copy() {
		return new DDDVector(x, y, z);
	}
}