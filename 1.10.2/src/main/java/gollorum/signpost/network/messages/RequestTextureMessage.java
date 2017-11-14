package gollorum.signpost.network.messages;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class RequestTextureMessage implements IMessage {

	private int x,y,z;
	public EnumHand hand;
	public EnumFacing facing;
	public float hitX, hitY, hitZ;

	public RequestTextureMessage(){}

	public RequestTextureMessage(int x, int y, int z, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		this.x = x;
		this.y = y;
		this.z = z;
		this.hand = hand;
		this.facing = facing;
		this.hitX = hitX;
		this.hitY = hitY;
		this.hitZ = hitZ;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		hand = buf.readBoolean() ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
		facing = EnumFacing.VALUES[buf.readByte()];
		hitX = buf.readFloat();
		hitY = buf.readFloat();
		hitZ = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeBoolean(hand == EnumHand.MAIN_HAND);
		buf.writeByte(facing.getIndex());
		buf.writeFloat(hitX);
		buf.writeFloat(hitY);
		buf.writeFloat(hitZ);
	}

	public BlockPos toBlockPos() {
		return new BlockPos(x, y, z);
	}

}
