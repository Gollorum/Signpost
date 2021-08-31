package gollorum.signpost.minecraft.gui;

import gollorum.signpost.blockpartdata.types.PostBlockPart;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class PaintPostGui extends PaintBlockPartGui<PostBlockPart> {

    public PaintPostGui(PostTile tile, PostBlockPart post, UUID identifier) {
        super(tile, post, new PostBlockPart(post.getTexture()), identifier, post.getTexture());
    }

	public static void display(PostTile tile, PostBlockPart post, UUID identifier) {
        Minecraft.getInstance().setScreen(new PaintPostGui(tile, post, identifier));
	}

    @Override
    protected void setTexture(PostBlockPart part, ResourceLocation texture) {
        part.setTexture(texture);
    }

}
