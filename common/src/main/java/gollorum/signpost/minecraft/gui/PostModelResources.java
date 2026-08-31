package gollorum.signpost.minecraft.gui;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.PostBlock;
import net.minecraft.resources.Identifier;

public class PostModelResources {

    public static final String secondaryTexture = "secondary_texture";

    public static final Identifier postLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/post_only");

    public static final Identifier wideLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/small_wide_sign");
    public static final Identifier wideFlippedLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, wideLocation.getPath() + "_flipped");
    public static final Identifier shortLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/small_short_sign");
    public static final Identifier shortFlippedLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, shortLocation.getPath() + "_flipped");
    public static final Identifier largeLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/large_sign");
    public static final Identifier largeFlippedLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, largeLocation.getPath() + "_flipped");

    public static final Identifier wideOverlayLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/small_wide_sign_overlay");
    public static final Identifier wideOverlayFlippedLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, wideOverlayLocation.getPath() + "_flipped");
    public static final Identifier shortOverlayLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/small_short_sign_overlay");
    public static final Identifier shortOverlayFlippedLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, shortOverlayLocation.getPath() + "_flipped");
    public static final Identifier largeOverlayLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, "block/large_sign_overlay");
    public static final Identifier largeOverlayFlippedLocation = Identifier.fromNamespaceAndPath(Signpost.MOD_ID, largeOverlayLocation.getPath() + "_flipped");

    public static final Identifier[] all = new Identifier[] {
        postLocation,
        wideLocation, wideFlippedLocation, shortLocation, shortFlippedLocation, largeLocation, largeFlippedLocation,
        wideOverlayLocation, wideOverlayFlippedLocation, shortOverlayLocation, shortOverlayFlippedLocation, largeOverlayLocation, largeOverlayFlippedLocation
    };

}
