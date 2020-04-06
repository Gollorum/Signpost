package gollorum.signpost.network.messages;

import gollorum.signpost.blocks.tiles.PostPostTile;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.DoubleBaseInfo;
import gollorum.signpost.util.MyBlockPos;
import gollorum.signpost.util.Sign;
import gollorum.signpost.util.Sign.OverlayType;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.HashMap;
import java.util.Map.Entry;

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

		public String paint1;
		public String paint2;
		
		public String postPaint;
		
		public byte paintObjectIndex;
		
		public DoubleStringInt(
			String string1, String string2,
			int int1, int int2,
			boolean bool1, boolean bool2,
			OverlayType overlay1, OverlayType overlay2,
			boolean bool3, boolean bool4,
			String paint1, String paint2,
			String postPaint,
			byte paintObjectIndex
		) {
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
			this.paint1 = paint1;
			this.paint2 = paint2;
			this.postPaint = postPaint;
			this.paintObjectIndex = paintObjectIndex;
		}
	}
	
	public HashMap<MyBlockPos, DoubleStringInt> posts = new HashMap<>();

	public HashMap<MyBlockPos, DoubleBaseInfo> toPostMap(){
		HashMap<MyBlockPos, DoubleBaseInfo> postMap = new HashMap<>();
		for(Entry<MyBlockPos, DoubleStringInt> now: posts.entrySet()){
			BaseInfo base1 = PostHandler.getForceWSbyName(now.getValue().string1);
			BaseInfo base2 = PostHandler.getForceWSbyName(now.getValue().string2);
			ResourceLocation paint1 = SuperPostPostTile.stringToLoc(now.getValue().paint1);
			ResourceLocation paint2 = SuperPostPostTile.stringToLoc(now.getValue().paint2);
			ResourceLocation postPaint = SuperPostPostTile.stringToLoc(now.getValue().postPaint);
			DoubleBaseInfo neu = postMap.put(
				now.getKey(),
				new DoubleBaseInfo(
					new Sign(
						base1,
						now.getValue().int1,
						now.getValue().bool1,
						now.getValue().overlay1,
						now.getValue().bool3,
						paint1
					),
					new Sign(
						base2,
						now.getValue().int2,
						now.getValue().bool2,
						now.getValue().overlay2,
						now.getValue().bool4,
						paint2
					),
					postPaint
				)
			);
		}
		return postMap;
	}
	
	public SendAllPostBasesMessage(){}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(PostHandler.getPosts().size());
		for(Entry<MyBlockPos, DoubleBaseInfo> now: PostHandler.getPosts().entrySet()){
			now.getKey().toBytes(buf);
			ByteBufUtils.writeUTF8String(buf, ""+now.getValue().sign1.base);
			ByteBufUtils.writeUTF8String(buf, ""+now.getValue().sign2.base);
			buf.writeInt(now.getValue().sign1.rotation);
			buf.writeInt(now.getValue().sign2.rotation);
			buf.writeBoolean(now.getValue().sign1.flip);
			buf.writeBoolean(now.getValue().sign2.flip);
			ByteBufUtils.writeUTF8String(buf, ""+now.getValue().sign1.overlay);
			ByteBufUtils.writeUTF8String(buf, ""+now.getValue().sign2.overlay);
			buf.writeBoolean(now.getValue().sign1.point);
			buf.writeBoolean(now.getValue().sign2.point);
			ByteBufUtils.writeUTF8String(buf, SuperPostPostTile.locToString(now.getValue().sign1.paint));
			ByteBufUtils.writeUTF8String(buf, SuperPostPostTile.locToString(now.getValue().sign2.paint));
			ByteBufUtils.writeUTF8String(buf, SuperPostPostTile.locToString(now.getValue().postPaint));
			PostPostTile tile = (PostPostTile) now.getKey().getTile();
			if(tile!=null){
				if(now.getValue().equals(tile.getPaintObject())){
					buf.writeByte(1);
				}else if(now.getValue().sign1.equals(tile.getPaintObject())){
					buf.writeByte(2);
				}else if(now.getValue().sign2.equals(tile.getPaintObject())){
					buf.writeByte(3);
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
			posts.put(
				MyBlockPos.fromBytes(buf),
				new DoubleStringInt(
					ByteBufUtils.readUTF8String(buf),
					ByteBufUtils.readUTF8String(buf),
					buf.readInt(), buf.readInt(),
					buf.readBoolean(), buf.readBoolean(),
					OverlayType.get(ByteBufUtils.readUTF8String(buf)),
					OverlayType.get(ByteBufUtils.readUTF8String(buf)),
					buf.readBoolean(), buf.readBoolean(),
					ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf),
					ByteBufUtils.readUTF8String(buf),
					buf.readByte()
				)
			);
		}
	}

}
