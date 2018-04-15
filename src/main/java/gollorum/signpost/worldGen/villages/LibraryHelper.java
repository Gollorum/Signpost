package gollorum.signpost.worldGen.villages;

import java.util.Map;
import java.util.Set;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.math.tracking.DDDVector;

abstract class LibraryHelper {
	protected MyBlockPos villageLocation;

	protected LibraryHelper(MyBlockPos villageLocation) {
		this.villageLocation = villageLocation;
	}

	protected BaseInfo getBaseInfo(MyBlockPos stoneLocation){
		return PostHandler.getNativeWaystones().getByPos(stoneLocation);
	}
	
	protected int compareClosest(MyBlockPos pos1, MyBlockPos pos2, MyBlockPos target){
		return Double.compare(pos1.distance(target), pos2.distance(target));
	}

	protected double calcRot(MyBlockPos pos1, MyBlockPos pos2) {
		int dx = pos2.x-pos1.x;
		int dz = pos2.z-pos1.z;
		return (DDDVector.genAngle(dx, dz)+Math.toRadians(90+(dx<0&&dz>0?180:0)));
	}

	protected boolean angleTooLarge(double calcRot, double optimalRot) {
		double difference = Math.abs(optimalRot - calcRot) % (Math.PI * 2);
		double twisted = Math.abs(difference - Math.PI);
		return twisted < Math.PI*0.5;
	}
	
}