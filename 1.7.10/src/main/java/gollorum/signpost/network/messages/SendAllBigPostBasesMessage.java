package gollorum.signpost.network.messages;

import java.util.HashMap;
import java.util.Map.Entry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import gollorum.signpost.blocks.SuperPostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.BlockPos;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.Sign.OverlayType;
import gollorum.signpost.util.collections.Lurchpaerchensauna;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;

public class SendAllBigPostBasesMessage implements IMessage{

	public class BigStringInt{
		public String string;
		public int datInt;
		public boolean bool;
		public OverlayType overlay;
		public boolean bool2;
		public String[] strings;
		public ResourceLocation paint;
		
		public BigStringInt(String string, int datInt, boolean bool, OverlayType overlay, boolean bool2, String[] strings, ResourceLocation paint) {
			this.string = string;
			this.datInt = datInt;
			this.bool = bool;
			this.overlay = overlay;
			this.bool2 = bool2;
			this.strings = strings;
			this.paint = paint;
		}
	}
	
	public HashMap<BlockPos, BigStringInt> bigPosts = new HashMap<BlockPos, BigStringInt>();

	public Lurchpaerchensauna<BlockPos, BigBaseInfo> toPostMap(){
		Lurchpaerchensauna<BlockPos, BigBaseInfo> postMap = new Lurchpaerchensauna<BlockPos, BigBaseInfo>();
		for(Entry<BlockPos, BigStringInt> now: bigPosts.entrySet()){
			BaseInfo base = PostHandler.getWSbyName(now.getValue().string);
			postMap.put(now.getKey(), new BigBaseInfo(new Sign(base,
															   now.getValue().datInt,
															   now.getValue().bool,
															   now.getValue().overlay,
															   now.getValue().bool2,
															   now.getValue().paint),
														now.getValue().strings));
		}
		return postMap;
	}
	
	public SendAllBigPostBasesMessage(){}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(PostHandler.bigPosts.size());
		for(Entry<BlockPos, BigBaseInfo> now: PostHandler.bigPosts.entrySet()){
			now.getKey().toBytes(buf);
			ByteBufUtils.writeUTF8String(buf, ""+now.getValue().sign.base);
			buf.writeInt(now.getValue().sign.rotation);
			buf.writeBoolean(now.getValue().sign.flip);
			ByteBufUtils.writeUTF8String(buf, ""+now.getValue().sign.overlay);
			buf.writeBoolean(now.getValue().sign.point);
			buf.writeInt(now.getValue().description.length);
			for(String now2: now.getValue().description){
				ByteBufUtils.writeUTF8String(buf, now2);
			}
			ByteBufUtils.writeUTF8String(buf, SuperPostPostTile.LocToString(now.getValue().sign.paint));
		}
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		int c = buf.readInt();
		for(int i = 0; i<c; i++){
			bigPosts.put(BlockPos.fromBytes(buf), 
					new BigStringInt(ByteBufUtils.readUTF8String(buf),
										buf.readInt(),
										buf.readBoolean(),
										OverlayType.get(ByteBufUtils.readUTF8String(buf)),
										buf.readBoolean(),
										readDescription(buf),
										SuperPostPostTile.stringToLoc(ByteBufUtils.readUTF8String(buf))));
		}
	}

	private String[] readDescription(ByteBuf buf){
		String[] ret = new String[buf.readInt()];
		for(int i=0; i<ret.length; i++){
			ret[i] = ByteBufUtils.readUTF8String(buf);
		}
		return ret;
	}
	
}
