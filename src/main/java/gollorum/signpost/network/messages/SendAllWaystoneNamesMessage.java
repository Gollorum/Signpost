package gollorum.signpost.network.messages;

import java.util.Collection;
import java.util.HashSet;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class SendAllWaystoneNamesMessage implements IMessage {

	public Collection<String> waystones;

	public SendAllWaystoneNamesMessage(Collection<String> waystones) {
		this.waystones = waystones;
	}

	public SendAllWaystoneNamesMessage() {
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		int count = buf.readInt();
		waystones = new HashSet<String>(count);
		for (int i = 0; i < count; i++) {
			String name = ByteBufUtils.readUTF8String(buf);
			waystones.add(name);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(waystones.size());
		for (String name : waystones) {
			ByteBufUtils.writeUTF8String(buf, name);
		}
	}

}
