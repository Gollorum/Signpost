package gollorum.signpost.network.messages;

import gollorum.signpost.blocks.tiles.BigPostPostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.BigBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.Sign.OverlayType;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.HashMap;
import java.util.Map.Entry;

public class SendAllBigPostBasesMessage implements IMessage{

	public class BigStringInt{
		public String string;
		public int datInt;
		public boolean bool;
		public OverlayType overlay;
		public boolean bool2;
		public String[] strings;
		public ResourceLocation paint;
		public ResourceLocation postPaint;
		
		public byte paintObjectIndex;
		
		public BigStringInt(String string, int datInt, boolean bool, OverlayType overlay, boolean bool2, String[] strings, ResourceLocation paint, ResourceLocation postPaint,  byte paintObjectIndex) {
			this.string = string;
			this.datInt = datInt;
			this.bool = bool;
			this.overlay = overlay;
			this.bool2 = bool2;
			this.strings = strings;
			this.paint = paint;
			this.postPaint = postPaint;
			this.paintObjectIndex = paintObjectIndex;
		}
	}
	
	public HashMap<MyBlockPos, BigStringInt> bigPosts = new HashMap<MyBlockPos, BigStringInt>();

	public HashMap<MyBlockPos, BigBaseInfo> toPostMap(){
		HashMap<MyBlockPos, BigBaseInfo> postMap = new HashMap<MyBlockPos, BigBaseInfo>();
		for(Entry<MyBlockPos, BigStringInt> now: bigPosts.entrySet()){
			BaseInfo base = PostHandler.getForceWSbyName(now.getValue().string);
			postMap.put(now.getKey(), new BigBaseInfo(new Sign(
				base,
				now.getValue().datInt,
				now.getValue().bool,
				now.getValue().overlay,
				now.getValue().bool2,
				now.getValue().paint),
				now.getValue().strings,
				now.getValue().postPaint));
		}
		return postMap;
	}
	
	public SendAllBigPostBasesMessage(){}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(PostHandler.getBigPosts().size());
		for(Entry<MyBlockPos, BigBaseInfo> now: PostHandler.getBigPosts().entrySet()){
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
			ByteBufUtils.writeUTF8String(buf, SuperPostPostTile.locToString(now.getValue().sign.paint));
			ByteBufUtils.writeUTF8String(buf, SuperPostPostTile.locToString(now.getValue().postPaint));
			BigPostPostTile tile = (BigPostPostTile) now.getKey().getTile();
			if(tile!=null){
				if(now.getValue().equals(tile.getPaintObject())){
					buf.writeByte(1);
				}else if(now.getValue().sign.equals(tile.getPaintObject())){
					buf.writeByte(2);
				}else{
					buf.writeByte(0);
				}
			}else{
				buf.writeByte(0);
			}
		}
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		int c = buf.readInt();
		for(int i = 0; i<c; i++){
			bigPosts.put(
				MyBlockPos.fromBytes(buf),
				new BigStringInt(ByteBufUtils.readUTF8String(buf),
					buf.readInt(),
					buf.readBoolean(),
					OverlayType.get(ByteBufUtils.readUTF8String(buf)),
					buf.readBoolean(),
					readDescription(buf),
					SuperPostPostTile.stringToLoc(ByteBufUtils.readUTF8String(buf)),
					SuperPostPostTile.stringToLoc(ByteBufUtils.readUTF8String(buf)),
					buf.readByte()
				)
			);
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
