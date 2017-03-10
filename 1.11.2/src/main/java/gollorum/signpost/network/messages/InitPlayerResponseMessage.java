package gollorum.signpost.network.messages;

import gollorum.signpost.management.ConfigHandler;
import gollorum.signpost.management.ConfigHandler.SecurityLevel;
import gollorum.signpost.management.PostHandler;
import gollorum.signpost.util.BaseInfo;
import gollorum.signpost.util.StonedHashSet;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class InitPlayerResponseMessage implements IMessage{

	public StonedHashSet allWaystones = new StonedHashSet();

	public static boolean deactivateTeleportation;
	public static boolean interdimensional;
	public static int maxDist;
	public static String paymentItem;
	public static int costMult;

	public static SecurityLevel securityLevelWaystone;
	public static SecurityLevel securityLevelSignpost;
	
	public InitPlayerResponseMessage(){
		if(!ConfigHandler.deactivateTeleportation){
			allWaystones = PostHandler.allWaystones;
		}
		deactivateTeleportation = ConfigHandler.deactivateTeleportation;
		interdimensional = ConfigHandler.interdimensional;
		maxDist = ConfigHandler.maxDist;
		paymentItem = ConfigHandler.paymentItem;
		costMult = ConfigHandler.costMult;
		securityLevelWaystone = ConfigHandler.securityLevelWaystone;
		securityLevelSignpost = ConfigHandler.securityLevelSignpost;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(deactivateTeleportation);
		if(!ConfigHandler.deactivateTeleportation){
			buf.writeInt(allWaystones.size());
			for(BaseInfo now:allWaystones){
				now.toBytes(buf);
			}
		}
		buf.writeBoolean(interdimensional);
		buf.writeInt(maxDist);
		ByteBufUtils.writeUTF8String(buf, paymentItem);
		buf.writeInt(costMult);
		ByteBufUtils.writeUTF8String(buf, securityLevelWaystone.name());
		ByteBufUtils.writeUTF8String(buf, securityLevelSignpost.name());
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		deactivateTeleportation = buf.readBoolean();
		if(!deactivateTeleportation){
			allWaystones = new StonedHashSet();
			int c = buf.readInt();
			for(int i=0; i<c; i++){
				allWaystones.add(BaseInfo.fromBytes(buf));
			}
		}
		interdimensional = buf.readBoolean();
		maxDist = buf.readInt();
		paymentItem = ByteBufUtils.readUTF8String(buf);
		costMult = buf.readInt();
		securityLevelWaystone = ConfigHandler.SecurityLevel.valueOf(ByteBufUtils.readUTF8String(buf));
		securityLevelSignpost = ConfigHandler.SecurityLevel.valueOf(ByteBufUtils.readUTF8String(buf));
	}

}
