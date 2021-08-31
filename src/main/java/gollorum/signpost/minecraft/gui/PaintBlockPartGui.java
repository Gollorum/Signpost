package gollorum.signpost.minecraft.gui;

import com.google.common.collect.Streams;
import gollorum.signpost.minecraft.block.tiles.PostTile;
import gollorum.signpost.minecraft.gui.utils.ExtendedScreen;
import gollorum.signpost.minecraft.gui.utils.Point;
import gollorum.signpost.minecraft.gui.utils.Rect;
import gollorum.signpost.minecraft.gui.widgets.GuiBlockPartRenderer;
import gollorum.signpost.minecraft.gui.widgets.ItemButton;
import gollorum.signpost.minecraft.gui.widgets.SpriteSelectionButton;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.Tuple;
import gollorum.signpost.utils.math.Angle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PaintBlockPartGui<T extends BlockPart<T>> extends ExtendedScreen {

    private final PostTile tile;
    private final int maxBlocksPerRow = 9;

    private List<SpriteSelectionButton> textureButtons = new ArrayList<>();
    protected final T part;
    protected final T displayPart;
    protected TextureAtlasSprite oldSprite;
    private final UUID identifier;
    private Function<ResourceLocation, TextureAtlasSprite> atlasSpriteGetter;

    public PaintBlockPartGui(PostTile tile, T part, T displayPart, UUID identifier, ResourceLocation oldTexture) {
        super(new TextComponent("Paint Post"));
        this.tile = tile;
        this.part = part;
        this.displayPart = displayPart;
        atlasSpriteGetter = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        oldSprite = spriteFrom(oldTexture);
        this.identifier = identifier;
    }
    
    protected TextureAtlasSprite spriteFrom(ResourceLocation loc) {
        return atlasSpriteGetter.apply(loc);
    }

    @Override
    protected void init() {
        super.init();

        List<Tuple<List<TextureAtlasSprite>, ItemStack>> blocksToRender = getMinecraft().player.getInventory().items.stream()
            .filter(i -> !i.isEmpty() && i.getItem() instanceof BlockItem)
            .map(i -> new ItemStack(i.getItem()))
            .distinct()
            .map(is -> Tuple.of(allSpritesFor((BlockItem) is.getItem()), is))
            .filter(p -> p._1.size() > 0)
            .collect(Collectors.toList());

        int rows = (blocksToRender.size() + 8) / 9;

        for(int y = 0; y < rows; y++) {
            int rowWidth = y == rows - 1 ? ((blocksToRender.size() - 1) % 9) + 1 : maxBlocksPerRow;
            int top = (height * 3) / 4 + y * ItemButton.height;
            int left = width / 2 - (rowWidth * ItemButton.width / 2);
            for (int x = 0; x < rowWidth; x++) {
                Tuple<List<TextureAtlasSprite>, ItemStack> tuple = blocksToRender.get(x + y * 9);
                addRenderableWidget(new ItemButton(
                    left + x * ItemButton.width, top, Rect.XAlignment.Left, Rect.YAlignment.Bottom,
                    tuple._2,
                    b -> setupTextureButtonsFor(tuple._1),
                    itemRenderer, font
                ));
            }
        }

        addRenderableWidget(new GuiBlockPartRenderer(
            tile.getParts().stream()
                .map(p -> p.blockPart == part ? new BlockPartInstance(displayPart, p.offset) : p)
                .collect(Collectors.toList()),
            new Point(width / 2, height / 4),
            Angle.fromDegrees(getMinecraft().player.getYRot() + 180),
            Angle.fromDegrees(getMinecraft().player.getXRot()),
            64
        ));
    }

    private static final Direction[] faces = new Direction[]{null, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};

    private List<TextureAtlasSprite> allSpritesFor(BlockItem item) {
        Block block = ((BlockItem) item.asItem()).getBlock();
        BlockState state = block.defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        return Arrays.stream(faces)
            .flatMap(side -> model.getQuads(null, side, font.random).stream())
            .map(BakedQuad::getSprite)
            .distinct()
            .collect(Collectors.toList());
    }

    private void setupTextureButtonsFor(List<TextureAtlasSprite> sprites) {
        clearSelection();

        sprites = Streams.concat(
            Stream.of(oldSprite),
            sprites.stream()
        ).distinct().collect(Collectors.toList());

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
                imgButton -> setTexture(displayPart, sprite.getName())
            );
            addRenderableWidget(newButton);
            textureButtons.add(newButton);
        }
    }

    protected void clearSelection() {
        for(SpriteSelectionButton button : textureButtons) {
            removeWidget(button);
        }
    }

    protected abstract void setTexture(T part, ResourceLocation texture);

    @Override
    public void onClose() {
        super.onClose();
        PacketHandler.sendToServer(new PostTile.PartMutatedEvent.Packet(
            new PostTile.TilePartInfo(tile, identifier),
            part.getMeta().write(displayPart),
            part.getMeta().identifier
        ));
    }
}
