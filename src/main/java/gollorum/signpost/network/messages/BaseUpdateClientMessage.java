package gollorum.signpost.network.messages;

import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class BaseUpdateClientMessage implements IMessage {

	public StonedHashSet waystones = new StonedHashSet();
	
	public BaseUpdateClientMessage(){
		waystones = PostHandler.getNativeWaystones();
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(waystones.size());
		for(BaseInfo now: waystones){
			now.toBytes(buf);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		waystones = new StonedHashSet();
		int c = buf.readInt();
		for(int i = 0; i<c; i++){
			waystones.add(BaseInfo.fromBytes(buf));
		}
	}

}
