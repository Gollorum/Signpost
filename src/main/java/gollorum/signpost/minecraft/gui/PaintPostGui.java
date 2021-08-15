package gollorum.signpost.minecraft.gui;

import com.google.common.collect.Streams;
import gollorum.signpost.blockpartdata.types.Post;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.*;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.BlockPartInstance;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PaintPostGui extends ExtendedScreen {

    private final PostTile tile;
    private final Post post;
    private final Post displayPost;
    private final int maxBlocksPerRow = 9;

    private List<SpriteSelectionButton> textureButtons = new ArrayList<>();
    private final TextureAtlasSprite oldSprite;
    private final UUID identifier;

    public PaintPostGui(PostTile tile, Post post, UUID identifier) {
        super(new StringTextComponent("Paint Post"));
        this.tile = tile;
        this.post = post;
        displayPost = new Post(post.getTexture());
        oldSprite = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(post.getTexture());
        this.identifier = identifier;
    }

	public static void display(PostTile tile, Post post, UUID identifier) {
        Minecraft.getInstance().displayGuiScreen(new PaintPostGui(tile, post, identifier));
	}

    @Override
    protected void init() {
        super.init();

        List<ItemStack> blocksToRender = getMinecraft().player.inventory.mainInventory.stream()
            .filter(i -> !i.isEmpty() && i.getItem() instanceof BlockItem)
            .map(i -> new ItemStack(i.getItem()))
            .distinct()
            .collect(Collectors.toList());

        int rows = (blocksToRender.size() + 8) / 9;

        for(int y = 0; y < rows; y++) {
            int rowWidth = y == rows - 1 ? blocksToRender.size() % 9 : maxBlocksPerRow;
            int top = (height * 3) / 4 + y * ItemButton.height;
            int left = width / 2 - (rowWidth * ItemButton.width / 2);
            for (int x = 0; x < rowWidth; x++) {
                addButton(new ItemButton(
                    left + x * ItemButton.width, top, Rect.XAlignment.Left, Rect.YAlignment.Bottom,
                    blocksToRender.get(x + y * 9),
                    this::setupTextureButtonsFor,
                    itemRenderer, font
                ));
            }
        }

        addButton(new GuiBlockPartRenderer(
            tile.getParts().stream()
                .map(p -> p.blockPart == post ? new BlockPartInstance(displayPost, p.offset) : p)
                .collect(Collectors.toList()),
            new Point(width / 2, height / 4), 30, 15, 64));
    }

    private static final Direction[] faces = new Direction[]{null, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};

    private void setupTextureButtonsFor(ItemButton b) {
        Block block = ((BlockItem) b.stack.getItem()).getBlock();
        BlockState state = block.getDefaultState();
        IBakedModel model = Minecraft.getInstance().getBlockRendererDispatcher().getModelForState(state);
        List<TextureAtlasSprite> sprites = Streams.concat(
            Stream.of(oldSprite),
            Arrays.stream(faces)
                .flatMap(side -> model.getQuads(null, side, font.random).stream())
                .map(BakedQuad::getSprite)
            ).distinct()
            .collect(Collectors.toList());
        removeButtons(textureButtons);

        int spriteButtonSize = 30;
        int centerY = height / 2 + 10;
        int left = width / 2 - (sprites.size() * spriteButtonSize / 2);
        for (int x = 0; x < sprites.size(); x++) {
            TextureAtlasSprite sprite = sprites.get(x);
            SpriteSelectionButton newButton = new SpriteSelectionButton(
                new Rect(
                    new Point(left + x * spriteButtonSize, centerY),
                    spriteButtonSize, spriteButtonSize,
                    Rect.XAlignment.Left, Rect.YAlignment.Center
                ),
                sprite,
                imgButton -> displayPost.setTexture(sprite.getName())
            );
            addButton(newButton);
            textureButtons.add(newButton);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        PacketHandler.sendToServer(new PostTile.PartMutatedEvent.Packet(
            new PostTile.TilePartInfo(tile, identifier),
            Post.METADATA.write(displayPost),
            Post.METADATA.identifier
        ));
    }
}
