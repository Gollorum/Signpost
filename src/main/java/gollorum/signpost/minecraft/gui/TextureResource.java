package gollorum.signpost.minecraft.gui;

import gollorum.signpost.Signpost;
import net.minecraft.util.ResourceLocation;

public class TextureResource {

    public static final TextureResource waystoneNameField = new TextureResource(
        "textures/gui/base_gui.png", new TextureSize(50, 11)
    );

    public static final TextureResource post = new TextureResource(
        "textures/gui/post.png", new TextureSize(4, 16)
    );

    public static final TextureResource expandContract = new TextureResource(
        "textures/gui/expand_contract.png", new TextureSize(11, 11), new TextureSize(22, 22)
    );

    public static final TextureResource background = new TextureResource(
        "textures/gui/background.png", new TextureSize(16, 16)
    );

    public static final TextureResource signTypeSelection = new TextureResource(
        "textures/gui/sign_type_selection.png", new TextureSize(58, 44), new TextureSize(174, 88)
    );

    public static final TextureResource largeSign = new TextureResource(
        "textures/gui/large_sign.png", new TextureSize(22, 16)
    );

    public static final TextureResource smallShortSign = new TextureResource(
        "textures/gui/small_slim_sign.png", new TextureSize(20, 16)
    );

    public static final TextureResource smallWideSign = new TextureResource(
        "textures/gui/small_wide_sign.png", new TextureSize(25, 16)
    );

    public final ResourceLocation location;
    public final TextureSize size;
    public final TextureSize fileSize;

    public TextureResource(ResourceLocation location, TextureSize size, TextureSize fileSize) {
        this.location = location;
        this.size = size;
        this.fileSize = fileSize;
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
