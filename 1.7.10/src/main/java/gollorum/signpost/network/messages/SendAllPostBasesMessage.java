package gollorum.signpost.network.messages;

import java.util.HashMap;
import java.util.Map.Entry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import io.netty.buffer.ByteBuf;

public class SendAllPostBasesMessage implements IMessage{

	public class DoubleStringInt{
		public String string1;
		public String string2;
		
		public int int1;
		public int int2;
		
		public boolean bool1;
		public boolean bool2;		
		
		public DoubleStringInt(String string1, String string2, int int1, int int2, boolean bool1, boolean bool2) {
			this.string1 = string1;
			this.string2 = string2;
			this.int1 = int1;
			this.int2 = int2;
			this.bool1 = bool1;
			this.bool2 = bool2;
		}
	}
	
	public HashMap<BlockPos, DoubleStringInt> posts = new HashMap<BlockPos, DoubleStringInt>();
	
	public SendAllPostBasesMessage(){
		for(Entry<BlockPos, DoubleBaseInfo> now: PostHandler.posts.entrySet()){
			posts.put(now.getKey(), new DoubleStringInt(""+now.getValue().base1,
														""+now.getValue().base2, 
														now.getValue().rotation1, 
														now.getValue().rotation2, 
														now.getValue().flip1, 
														now.getValue().flip2));
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(posts.size());
		for(Entry<BlockPos, DoubleStringInt> now: posts.entrySet()){
				now.getKey().toBytes(buf);
				ByteBufUtils.writeUTF8String(buf, now.getValue().string1);
				ByteBufUtils.writeUTF8String(buf, now.getValue().string2);
				buf.writeInt(now.getValue().int1);
				buf.writeInt(now.getValue().int2);
				buf.writeBoolean(now.getValue().bool1);
				buf.writeBoolean(now.getValue().bool2);
		}
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		int c = buf.readInt();
		for(int i = 0; i<c; i++){
			posts.put(BlockPos.fromBytes(buf), 
					new DoubleStringInt(ByteBufUtils.readUTF8String(buf), 
										ByteBufUtils.readUTF8String(buf),
										buf.readInt(), buf.readInt(),
										buf.readBoolean(), buf.readBoolean()));
		}
	}

}
