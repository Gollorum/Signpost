package gollorum.signpost.util.math.tracking;

public class Cuboid extends Body {
	
	public Cuboid(DDDVector pos, DDDVector edges, DDDVector rotation){
		super(6);
		DDDVector xEdge = edges.xVec().rotate(rotation);
		DDDVector yEdge = edges.yVec().rotate(rotation);
		DDDVector zEdge = edges.zVec().rotate(rotation);
		add(new Rectangle(pos, xEdge, yEdge));
		add(new Rectangle(pos, xEdge, zEdge));
		add(new Rectangle(pos, yEdge, zEdge));
		
		pos = pos.add(xEdge).add(yEdge).add(zEdge);		
		xEdge = xEdge.neg();
		yEdge = yEdge.neg();
		zEdge = zEdge.neg();
		add(new Rectangle(pos, xEdge, yEdge));
		add(new Rectangle(pos, xEdge, zEdge));
		add(new Rectangle(pos, yEdge, zEdge));
		
	}

	public Cuboid(DDDVector pos, DDDVector edges, double rotation){
		this(pos, edges, new DDDVector(0, rotation, 0));
	}
	
	public Cuboid(DDDVector pos, DDDVector edges, double rotation, DDDVector rotationOffset){
		this(rotationOffset.add(pos.substract(rotationOffset).rotY(rotation+Math.PI)), edges, rotation);
	}

}

