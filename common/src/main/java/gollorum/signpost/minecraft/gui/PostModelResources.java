package gollorum.signpost.minecraft.gui;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.resources.ResourceLocation;

public class PostModelResources {

    public static final String texturePost = "post";
    public static final String textureSign = "texture";
    public static final ResourceLocation mainTextureMarker = PostBlock.ModelType.Oak.mainTexture.location();
    public static final String secondaryTexture = "secondary_texture";

    public static final ResourceLocation previewLocation = new ResourceLocation(Signpost.MOD_ID, "block/post_preview");

    public static final ResourceLocation postLocation = new ResourceLocation(Signpost.MOD_ID, "block/post_only");

    public static final ResourceLocation wideLocation = new ResourceLocation(Signpost.MOD_ID, "block/small_wide_sign");
    public static final ResourceLocation wideFlippedLocation = new ResourceLocation(Signpost.MOD_ID, wideLocation.getPath() + "_flipped");
    public static final ResourceLocation shortLocation = new ResourceLocation(Signpost.MOD_ID, "block/small_short_sign");
    public static final ResourceLocation shortFlippedLocation = new ResourceLocation(Signpost.MOD_ID, shortLocation.getPath() + "_flipped");
    public static final ResourceLocation largeLocation = new ResourceLocation(Signpost.MOD_ID, "block/large_sign");
    public static final ResourceLocation largeFlippedLocation = new ResourceLocation(Signpost.MOD_ID, largeLocation.getPath() + "_flipped");

    public static final ResourceLocation wideOverlayLocation = new ResourceLocation(Signpost.MOD_ID, "block/small_wide_sign_overlay");
    public static final ResourceLocation wideOverlayFlippedLocation = new ResourceLocation(Signpost.MOD_ID, wideOverlayLocation.getPath() + "_flipped");
    public static final ResourceLocation shortOverlayLocation = new ResourceLocation(Signpost.MOD_ID, "block/small_short_sign_overlay");
    public static final ResourceLocation shortOverlayFlippedLocation = new ResourceLocation(Signpost.MOD_ID, shortOverlayLocation.getPath() + "_flipped");
    public static final ResourceLocation largeOverlayLocation = new ResourceLocation(Signpost.MOD_ID, "block/large_sign_overlay");
    public static final ResourceLocation largeOverlayFlippedLocation = new ResourceLocation(Signpost.MOD_ID, largeOverlayLocation.getPath() + "_flipped");

    public static final ResourceLocation[] all = new ResourceLocation[] {
        postLocation,
        wideLocation, wideFlippedLocation, shortLocation, shortFlippedLocation, largeLocation, largeFlippedLocation,
        wideOverlayLocation, wideOverlayFlippedLocation, shortOverlayLocation, shortOverlayFlippedLocation, largeOverlayLocation, largeOverlayFlippedLocation
    };

}
