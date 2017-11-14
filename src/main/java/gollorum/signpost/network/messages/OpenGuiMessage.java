package gollorum.signpost.network.messages;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class OpenGuiMessage implements IMessage {

	public int guiID;
	public int x, y, z;
	
	public OpenGuiMessage(){}
	
	public OpenGuiMessage(int guiID, int x, int y, int z){
		this.guiID = guiID;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(guiID);
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		guiID = buf.readInt();
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
	}

}
