package gollorum.signpost.minecraft.gui.utils;

import gollorum.signpost.Signpost;
import net.minecraft.resources.ResourceLocation;

public class TextureResource {

    public static final int defaultTextureSize = 16;

    public static final int wideOverlaySize = 32;
    public static final int shortOverlaySize = 32;
    public static final int largeOverlaySize = 32;

    public static final TextureResource waystoneNameField = new TextureResource(
        "textures/gui/base_gui.png", new TextureSize(50, 11)
    );

    public static final TextureResource post = new TextureResource(
        "textures/gui/post.png", new TextureSize(4, 16)
    );

    public static final TextureResource expandContract = new TextureResource(
        "textures/gui/expand_contract.png", new TextureSize(11, 11), new TextureSize(22, 22)
    );

    public static final TextureResource flipDirection = new TextureResource(
        "textures/gui/flip_direction.png", new TextureSize(15, 15), new TextureSize(15, 30)
    );

    public static final TextureResource background = new TextureResource(
        "textures/gui/background.png", new TextureSize(16, 16)
    );

    public static final TextureResource signTypeSelection = new TextureResource(
        "textures/gui/sign_type_selection.png", new TextureSize(58, 44), new TextureSize(58, 88)
    );

    public static final TextureResource itemBackground = new TextureResource(
        new ResourceLocation("textures/gui/widgets.png"), new TextureSize(22, 22),
        new TextureSize(256, 256), new TextureSize(60, 23)
    );

    public static final TextureResource edit = new TextureResource(
        "textures/gui/edit.png", new TextureSize(15, 15), new TextureSize(15, 30)
    );

    public static final ResourceLocation waystoneTextureLocation = new ResourceLocation(Signpost.MOD_ID, "texture/block/waystone");

    public final ResourceLocation location;
    public final TextureSize size;
    public final TextureSize fileSize;
    public final TextureSize offset;

    public TextureResource(ResourceLocation location, TextureSize size, TextureSize fileSize) {
        this.location = location;
        this.size = size;
        this.fileSize = fileSize;
        offset = TextureSize.zero;
    }

    public TextureResource(ResourceLocation location, TextureSize size, TextureSize fileSize, TextureSize offset) {
        this.location = location;
        this.size = size;
        this.fileSize = fileSize;
        this.offset = offset;
    }

    public TextureResource(String relativeLocation, TextureSize size, TextureSize fileSize) {
        this(new ResourceLocation(Signpost.MOD_ID, relativeLocation), size, fileSize);
    }

    public TextureResource(ResourceLocation location, TextureSize size) {
        this(location, size, size);
    }

    public TextureResource(String relativeLocation, TextureSize size) {
        this(relativeLocation, size, size);
    }

}
