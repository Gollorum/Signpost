package gollorum.signpost.util;

public class DoubleBaseInfo {

	public BaseInfo base1;
	public BaseInfo base2;
	
	public int rotation1 = 0;
	public int rotation2 = 45;

	public boolean flip1;
	public boolean flip2;
	
	public DoubleBaseInfo(BaseInfo base1, BaseInfo base2, int int1, int int2, boolean flip1, boolean flip2) {
		this.base1 = base1;
		this.base2 = base2;
		rotation1 = int1;
		rotation2 = int2;
		this.flip1 = flip1;
		this.flip2 = flip2;
	}
	
	@Override
	public String toString(){
		return base1+":"+rotation1+"/"+flip1+" and "+base1+":"+rotation1+"/"+flip1;
	}
	
}
