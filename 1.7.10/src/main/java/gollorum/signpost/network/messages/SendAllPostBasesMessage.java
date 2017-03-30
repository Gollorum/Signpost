package gollorum.signpost.network.messages;

import java.util.HashMap;
import java.util.Map.Entry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.DoubleBaseInfo.OverlayType;
import gollorum.signpost.util.collections.Lurchpaerchensauna;
import io.netty.buffer.ByteBuf;

public class SendAllPostBasesMessage implements IMessage{

	public class DoubleStringInt{
		public String string1;
		public String string2;
		
		public int int1;
		public int int2;

		public boolean bool1;
		public boolean bool2;

		public OverlayType overlay1;
		public OverlayType overlay2;
		
		public boolean bool3;
		public boolean bool4;
		
		public DoubleStringInt(String string1, String string2, int int1, int int2, boolean bool1, boolean bool2, OverlayType overlay1, OverlayType overlay2, boolean bool3, boolean bool4) {
			this.string1 = string1;
			this.string2 = string2;
			this.int1 = int1;
			this.int2 = int2;
			this.bool1 = bool1;
			this.bool2 = bool2;
			this.overlay1 = overlay1;
			this.overlay2 = overlay2;
			this.bool3 = bool3;
			this.bool4 = bool4;
		}
	}
	
	public HashMap<BlockPos, DoubleStringInt> posts = new HashMap<BlockPos, DoubleStringInt>();

	public Lurchpaerchensauna<BlockPos, DoubleBaseInfo> toPostMap(){
		Lurchpaerchensauna<BlockPos, DoubleBaseInfo> postMap = new Lurchpaerchensauna<BlockPos, DoubleBaseInfo>();
		for(Entry<BlockPos, DoubleStringInt> now: posts.entrySet()){
			BaseInfo base1 = PostHandler.getWSbyName(now.getValue().string1);
			BaseInfo base2 = PostHandler.getWSbyName(now.getValue().string2);
			postMap.put(now.getKey(), new DoubleBaseInfo(base1, base2,
														 now.getValue().int1, now.getValue().int2,
														 now.getValue().bool1, now.getValue().bool2,
														 now.getValue().overlay1, now.getValue().overlay2,
														 now.getValue().bool3, now.getValue().bool4));
		}
		return postMap;
	}
	
	public SendAllPostBasesMessage(){}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(PostHandler.posts.size());
		for(Entry<BlockPos, DoubleBaseInfo> now: PostHandler.posts.entrySet()){
				now.getKey().toBytes(buf);
				ByteBufUtils.writeUTF8String(buf, ""+now.getValue().base1);
				ByteBufUtils.writeUTF8String(buf, ""+now.getValue().base2);
				buf.writeInt(now.getValue().rotation1);
				buf.writeInt(now.getValue().rotation2);
				buf.writeBoolean(now.getValue().flip1);
				buf.writeBoolean(now.getValue().flip2);
				ByteBufUtils.writeUTF8String(buf, ""+now.getValue().overlay1);
				ByteBufUtils.writeUTF8String(buf, ""+now.getValue().overlay2);
				buf.writeBoolean(now.getValue().point1);
				buf.writeBoolean(now.getValue().point2);
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
										buf.readBoolean(), buf.readBoolean(),
										OverlayType.get(ByteBufUtils.readUTF8String(buf)),
										OverlayType.get(ByteBufUtils.readUTF8String(buf)),
										buf.readBoolean(), buf.readBoolean()));
		}
	}

}
