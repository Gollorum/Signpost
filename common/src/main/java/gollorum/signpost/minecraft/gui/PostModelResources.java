package gollorum.signpost.minecraft.gui;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.resources.ResourceLocation;

public class PostModelResources {

    public static final String secondaryTexture = "secondary_texture";

    public static final ResourceLocation postLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/post_only");

    public static final ResourceLocation wideLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/small_wide_sign");
    public static final ResourceLocation wideFlippedLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, wideLocation.getPath() + "_flipped");
    public static final ResourceLocation shortLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/small_short_sign");
    public static final ResourceLocation shortFlippedLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, shortLocation.getPath() + "_flipped");
    public static final ResourceLocation largeLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/large_sign");
    public static final ResourceLocation largeFlippedLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, largeLocation.getPath() + "_flipped");

    public static final ResourceLocation wideOverlayLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/small_wide_sign_overlay");
    public static final ResourceLocation wideOverlayFlippedLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, wideOverlayLocation.getPath() + "_flipped");
    public static final ResourceLocation shortOverlayLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/small_short_sign_overlay");
    public static final ResourceLocation shortOverlayFlippedLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, shortOverlayLocation.getPath() + "_flipped");
    public static final ResourceLocation largeOverlayLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, "block/large_sign_overlay");
    public static final ResourceLocation largeOverlayFlippedLocation = ResourceLocation.fromNamespaceAndPath(Signpost.MOD_ID, largeOverlayLocation.getPath() + "_flipped");

    public static final ResourceLocation[] all = new ResourceLocation[] {
        postLocation,
        wideLocation, wideFlippedLocation, shortLocation, shortFlippedLocation, largeLocation, largeFlippedLocation,
        wideOverlayLocation, wideOverlayFlippedLocation, shortOverlayLocation, shortOverlayFlippedLocation, largeOverlayLocation, largeOverlayFlippedLocation
    };

}
