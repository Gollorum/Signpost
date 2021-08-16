package gollorum.signpost.minecraft.gui;

import gollorum.signpost.blockpartdata.types.Post;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

public class PaintPostGui extends PaintBlockPartGui<Post> {

    public PaintPostGui(PostTile tile, Post post, UUID identifier) {
        super(tile, post, new Post(post.getTexture()), identifier, post.getTexture());
    }

	public static void display(PostTile tile, Post post, UUID identifier) {
        Minecraft.getInstance().displayGuiScreen(new PaintPostGui(tile, post, identifier));
	}

    @Override
    protected void setTexture(Post part, ResourceLocation texture) {
        part.setTexture(texture);
    }

}
