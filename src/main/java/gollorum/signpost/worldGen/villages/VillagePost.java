package gollorum.signpost.worldGen.villages;

import java.util.List;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Sign;
import net.minecraft.nbt.NBTTagCompound;

class VillagePost {
	private MyBlockPos topSignPosition;
	public Double desiredRotation;

	public VillagePost(MyBlockPos topSignPosition, Double desiredRotation) {
		this.topSignPosition = topSignPosition;
		this.desiredRotation = desiredRotation;
	}

	@Override
	public String toString() {
		return topSignPosition.toString();
	}

	public List<Sign> getSigns() {
		return PostHandler.getSigns(topSignPosition);
	}

	public MyBlockPos getTopSignPosition() {
		return topSignPosition;
	}

	NBTTagCompound save() {
		NBTTagCompound compound = new NBTTagCompound();
		topSignPosition.writeToNBT(compound);
		compound.setDouble("DesiredRotation", desiredRotation);
		return compound;
	}

	static VillagePost load(NBTTagCompound compound) {
		MyBlockPos topSignPosition = MyBlockPos.readFromNBT(compound);
		double desiredRotation = compound.getDouble("DesiredRotation");
		return new VillagePost(topSignPosition, desiredRotation);
	}
}