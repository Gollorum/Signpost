package gollorum.signpost.network.handlers;

import gollorum.signpost.blocks.SuperPostPost;
import gollorum.signpost.blocks.tiles.SuperPostPostTile;
import gollorum.signpost.network.messages.RequestTextureMessage;
import gollorum.signpost.util.TextureHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RequestTextureHandler implements IMessageHandler<RequestTextureMessage, IMessage>{

	@Override
	public IMessage onMessage(RequestTextureMessage message, MessageContext ctx) {
		TextureHelper.instance().setTexture(message.toBlockPos(), message.hand, message.facing, message.hitX, message.hitY, message.hitZ);
		return null;
	}

}
