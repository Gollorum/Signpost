package gollorum.signpost.worldGen.villages;

import gollorum.signpost.util.MyBlockPos;
import net.minecraft.nbt.NBTTagCompound;

class VillagePost{
	public MyBlockPos topSignPosition;
	public Double desiredRotation;
	public VillagePost(MyBlockPos topSignPosition, Double desiredRotation) {
		this.topSignPosition = topSignPosition;
		this.desiredRotation = desiredRotation;
	}
	@Override
	public String toString(){
		return topSignPosition.toString();
	}
	
	NBTTagCompound save(){
		NBTTagCompound compound = new NBTTagCompound();
		topSignPosition.writeToNBT(compound);
		compound.setDouble("DesiredRotation", desiredRotation);
		return compound;
	}
	
	static VillagePost load(NBTTagCompound compound){
		MyBlockPos topSignPosition = MyBlockPos.readFromNBT(compound);
		double desiredRotation = compound.getDouble("DesiredRotation");
		return new VillagePost(topSignPosition, desiredRotation);
	}
}