package gollorum.signpost.network.messages;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class InitPlayerResponseMessage implements IMessage{

	public StonedHashSet allWaystones = new StonedHashSet();
	
	public InitPlayerResponseMessage(){
		allWaystones = PostHandler.allWaystones;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(allWaystones.size());
		for(BaseInfo now:allWaystones){
			now.toBytes(buf);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		allWaystones = new StonedHashSet();
		int c = buf.readInt();
		for(int i=0; i<c; i++){
			allWaystones.add(BaseInfo.fromBytes(buf));
		}
	}

}
