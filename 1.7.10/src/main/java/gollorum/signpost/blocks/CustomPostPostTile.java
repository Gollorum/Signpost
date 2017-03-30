package gollorum.signpost.blocks;

import gollorum.signpost.SPEventHandler;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.network.NetworkHandler;
import gollorum.signpost.network.messages.SendPostBasesMessage;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.BoolRun;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.DoubleBaseInfo.OverlayType;
import net.minecraft.nbt.NBTTagCompound;

public class CustomPostPostTile extends PostPostTile {

	public float uMin1;
	public float uMax1;
	public float vMin1;
	public float vMax1;

	public float uMin2;
	public float uMax2;
	public float vMin2;
	public float vMax2;
	
	public CustomPostPostTile(){}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		DoubleBaseInfo bases = getBases();
		tagCompound.setFloat("uMin1", uMin1);
		tagCompound.setFloat("uMax1", uMax1);
		tagCompound.setFloat("vMin1", vMin1);
		tagCompound.setFloat("vMax1", vMax1);
		
		tagCompound.setFloat("uMin2", uMin2);
		tagCompound.setFloat("uMax2", uMax2);
		tagCompound.setFloat("vMin2", vMin2);
		tagCompound.setFloat("vMax2", vMax2);
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		uMin1 = tagCompound.getFloat("uMin1");
		uMax1 = tagCompound.getFloat("uMax1");
		vMin1 = tagCompound.getFloat("vMin1");
		vMax1 = tagCompound.getFloat("vMax1");
		
		uMin2 = tagCompound.getFloat("uMin2");
		uMax2 = tagCompound.getFloat("uMax2");
		vMin2 = tagCompound.getFloat("vMin2");
		vMax2 = tagCompound.getFloat("vMax2");
	}
	
}
