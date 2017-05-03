package gollorum.signpost.util;

public class DoubleBaseInfo {

	public Sign sign1;
	public Sign sign2;


	public DoubleBaseInfo(){
		this.sign1 = new Sign();
		this.sign2 = new Sign();
	}
	
	public DoubleBaseInfo(Sign sign1, Sign sign2){
		this.sign1 = sign1;
		this.sign2 = sign2;
	}
	
}
