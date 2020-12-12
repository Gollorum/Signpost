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

    public static final TextureResource flipDirection = new TextureResource(
        "textures/gui/flip_direction.png", new TextureSize(15, 15), new TextureSize(15, 30)
    );

    public static final TextureResource background = new TextureResource(
        "textures/gui/background.png", new TextureSize(16, 16)
    );

    public static final TextureResource signTypeSelection = new TextureResource(
        "textures/gui/sign_type_selection.png", new TextureSize(58, 44), new TextureSize(58, 88)
    );

    public static final class SignGui {

        public static final class Short {

            private static final String prefix = "short_";
            private static final TextureSize size = new TextureSize(20, 16);

            public static final TextureResource Acacia = new TextureResource(
                "textures/gui/" + prefix + "acacia.png", size
            );

            public static final TextureResource Birch = new TextureResource(
                "textures/gui/" + prefix + "birch.png", size
            );

            public static final TextureResource DarkOak = new TextureResource(
                "textures/gui/" + prefix + "dark_oak.png", size
            );

            public static final TextureResource Iron = new TextureResource(
                "textures/gui/" + prefix + "iron.png", size
            );

            public static final TextureResource Jungle = new TextureResource(
                "textures/gui/" + prefix + "jungle.png", size
            );

            public static final TextureResource Oak = new TextureResource(
                "textures/gui/" + prefix + "oak.png", size
            );

            public static final TextureResource Spruce = new TextureResource(
                "textures/gui/" + prefix + "spruce.png", size
            );

            public static final TextureResource Stone = new TextureResource(
                "textures/gui/" + prefix + "spruce.png", size
            );

        }

        public static final class Wide {

            private static final String prefix = "wide_";
            private static final TextureSize size = new TextureSize(25, 16);

            public static final TextureResource Acacia = new TextureResource(
                "textures/gui/" + prefix + "acacia.png", size
            );

            public static final TextureResource Birch = new TextureResource(
                "textures/gui/" + prefix + "birch.png", size
            );

            public static final TextureResource DarkOak = new TextureResource(
                "textures/gui/" + prefix + "dark_oak.png", size
            );

            public static final TextureResource Iron = new TextureResource(
                "textures/gui/" + prefix + "iron.png", size
            );

            public static final TextureResource Jungle = new TextureResource(
                "textures/gui/" + prefix + "jungle.png", size
            );

            public static final TextureResource Oak = new TextureResource(
                "textures/gui/" + prefix + "oak.png", size
            );

            public static final TextureResource Spruce = new TextureResource(
                "textures/gui/" + prefix + "spruce.png", size
            );

            public static final TextureResource Stone = new TextureResource(
                "textures/gui/" + prefix + "spruce.png", size
            );

        }

        public static final class Large {

            private static final String prefix = "large_";
            private static final TextureSize size = new TextureSize(22, 16);

            public static final TextureResource Acacia = new TextureResource(
                "textures/gui/" + prefix + "acacia.png", size
            );

            public static final TextureResource Birch = new TextureResource(
                "textures/gui/" + prefix + "birch.png", size
            );

            public static final TextureResource DarkOak = new TextureResource(
                "textures/gui/" + prefix + "dark_oak.png", size
            );

            public static final TextureResource Iron = new TextureResource(
                "textures/gui/" + prefix + "iron.png", size
            );

            public static final TextureResource Jungle = new TextureResource(
                "textures/gui/" + prefix + "jungle.png", size
            );

            public static final TextureResource Oak = new TextureResource(
                "textures/gui/" + prefix + "oak.png", size
            );

            public static final TextureResource Spruce = new TextureResource(
                "textures/gui/" + prefix + "spruce.png", size
            );

            public static final TextureResource Stone = new TextureResource(
                "textures/gui/" + prefix + "spruce.png", size
            );

        }

    }

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
