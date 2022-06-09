package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallShortSignBlockPart;
import gollorum.signpost.blockpartdata.types.SmallWideSignBlockPart;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.utils.math.geometry.Vector3;
import gollorum.signpost.utils.modelGeneration.SignModelFactory;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Map;
import java.util.stream.Collectors;

public class PostModel extends BlockModelProvider {

    private static final String texturePost = "post";
    public static final String textureSign = "texture";
    public static final ResourceLocation mainTextureMarker = PostBlock.ModelType.Oak.mainTexture;
    public static final String secondaryTexture = "secondary_texture";

    private static final ResourceLocation previewLocation = new ResourceLocation(Signpost.MOD_ID, "block/post_preview");

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

    public final Map<PostBlock.Variant, BlockModelBuilder> allModels;

    private static final ModelBuilder.FaceRotation mainTextureRotation = ModelBuilder.FaceRotation.CLOCKWISE_90;
    private static final ModelBuilder.FaceRotation secondaryTextureRotation = ModelBuilder.FaceRotation.CLOCKWISE_90;

    private final BlockModelBuilder previewModel;

    public PostModel(DataGenerator generator, ExistingFileHelper fileHelper) {
        super(generator, Signpost.MOD_ID, fileHelper);
        previewModel = new BlockModelBuilder(previewLocation, fileHelper);
        allModels = PostBlock.AllVariants.stream().collect(Collectors.<PostBlock.Variant, PostBlock.Variant, BlockModelBuilder>toMap(
            i -> i,
            i -> new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + i.registryName), fileHelper)
        ));
        generator.addProvider(true, new Item(generator, fileHelper));
    }

    private class Item extends ItemModelProvider {

        public Item(DataGenerator generator, ExistingFileHelper existingFileHelper) {
            super(generator, Signpost.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            for (Map.Entry<PostBlock.Variant, BlockModelBuilder> entry : allModels.entrySet()) {
                getBuilder(entry.getKey().registryName)
                    .parent(new ModelFile.UncheckedModelFile("builtin/entity"))
                    .transforms()
                        .transform(ModelBuilder.Perspective.GUI)
                            .rotation(30, 35, 0)
                            .translation(0, 0, 0)
                            .scale(0.625f)
                        .end()
                        .transform(ModelBuilder.Perspective.GROUND)
                            .rotation(0, 0, 0)
                            .translation(0, 3, 0)
                            .scale(0.25f)
                        .end()
                        .transform(ModelBuilder.Perspective.FIXED)
                            .rotation(0, 180, 0)
                            .translation(0, 0, 0)
                            .scale(0.5f)
                        .end()
                        .transform(ModelBuilder.Perspective.THIRDPERSON_RIGHT)
                            .rotation(75, 135, 0)
                            .translation(0, 2.5f, 0)
                            .scale(0.375f)
                        .end()
                        .transform(ModelBuilder.Perspective.FIRSTPERSON_RIGHT)
                            .rotation(0, 315, 0)
                            .translation(0, 0, 0)
                            .scale(0.4f)
                        .end()
                        .transform(ModelBuilder.Perspective.FIRSTPERSON_LEFT)
                            .rotation(0, 315, 0)
                            .translation(0, 0, 0)
                            .scale(0.4f)
                        .end()
                    .end();
//                    .parent(entry.getValue());
            }
        }
    }

    @Override
    protected void registerModels() {
        BlockModelBuilder previewBuilder = getBuilder(previewLocation.toString())
            .parent(new ModelFile.ExistingModelFile(new ResourceLocation("block/block"), existingFileHelper))
            .transforms()
                .transform(ModelBuilder.Perspective.GUI)
                    .rotation(30, 315, 0)
                    .scale(0.625f)
                .end()
                .transform(ModelBuilder.Perspective.FIRSTPERSON_RIGHT)
                    .rotation(0, 315, 0)
                    .scale(0.4f)
                .end()
                .transform(ModelBuilder.Perspective.FIRSTPERSON_LEFT)
                    .rotation(0, 315, 0)
                    .scale(0.4f)
                .end()
            .end();
        makePostAt(new Vector3(8, 8, 8), previewBuilder);
        new SignModelFactory<String>().makeWideSign(new Vector3(8, 12, 8), "#" + textureSign, "#" + secondaryTexture)
            .build(previewBuilder, SignModelFactory.Builder.BlockModel);

        makePostAt(new Vector3(0, 8, 0), getBuilder(postLocation.toString()))
            .texture(texturePost, PostBlock.ModelType.Oak.postTexture);

        buildDefaultAndFlipped(
            new SignModelFactory<String>().makeWideSign("#" + textureSign, "#" + secondaryTexture),
            wideLocation,
            wideFlippedLocation
        );

        buildDefaultAndFlipped(
            new SignModelFactory<String>().makeShortSign("#" + textureSign, "#" + secondaryTexture),
            shortLocation,
            shortFlippedLocation
        );

        buildDefaultAndFlipped(
            new SignModelFactory<String>().makeLargeSign("#" + textureSign, "#" + secondaryTexture),
            largeLocation,
            largeFlippedLocation
        );

        buildDefaultAndFlippedOverlay(
            new SignModelFactory<String>().makeWideSignOverlay("#" + textureSign),
            wideOverlayLocation, wideOverlayFlippedLocation,
            Overlay.Gras.textureFor(SmallWideSignBlockPart.class)
        );
        buildDefaultAndFlippedOverlay(
            new SignModelFactory<String>().makeShortSignOverlay("#" + textureSign),
            shortOverlayLocation, shortOverlayFlippedLocation,
            Overlay.Gras.textureFor(SmallShortSignBlockPart.class)
        );
        buildDefaultAndFlippedOverlay(
            new SignModelFactory<String>().makeLargeSignOverlay("#" + textureSign),
            largeOverlayLocation, largeOverlayFlippedLocation,
            Overlay.Gras.textureFor(LargeSignBlockPart.class)
        );

        for(PostBlock.Variant variant : PostBlock.AllVariants) {
            getBuilder(variant.registryName)
                .parent(previewModel)
                .texture("particle", variant.type.postTexture)
                .texture(texturePost, variant.type.postTexture)
                .texture(textureSign, variant.type.mainTexture)
                .texture(secondaryTexture, variant.type.secondaryTexture);
        }
    }

    private void buildDefaultAndFlipped(SignModelFactory<String> factory, ResourceLocation main, ResourceLocation flipped) {
        factory.build(getBuilder(main.toString()), SignModelFactory.Builder.BlockModel)
            .texture(textureSign, mainTextureMarker)
            .texture(secondaryTexture, PostBlock.ModelType.Oak.secondaryTexture);
        factory.build(getBuilder(flipped.toString()), SignModelFactory.Builder.BlockModelFlipped)
            .texture(textureSign, mainTextureMarker)
            .texture(secondaryTexture, PostBlock.ModelType.Oak.secondaryTexture);
    }

    private void buildDefaultAndFlippedOverlay(SignModelFactory<String> factory, ResourceLocation main, ResourceLocation flipped, ResourceLocation texture) {
        factory.build(getBuilder(main.toString()), SignModelFactory.Builder.BlockModel)
            .texture(textureSign, texture);
        factory.build(getBuilder(flipped.toString()), SignModelFactory.Builder.BlockModelFlipped)
            .texture(textureSign, texture);
    }

    private static BlockModelBuilder makePostAt(Vector3 center, BlockModelBuilder builder) {
        builder
            .element()
                .from(center.x - 2, center.y - 8, center.z - 2)
                .to(center.x + 2, center.y + 8, center.z + 2)
            .face(Direction.SOUTH)
                .texture("#" + texturePost)
                .uvs(0, 0, 4, 16)
            .end()
            .face(Direction.EAST)
                .texture("#" + texturePost)
                .uvs(4, 0, 8, 16)
            .end()
            .face(Direction.NORTH)
                .texture("#" + texturePost)
                .uvs(8, 0, 12, 16)
            .end()
            .face(Direction.WEST)
                .texture("#" + texturePost)
                .uvs(12, 0, 16, 16)
            .end()
            .face(Direction.DOWN)
                .texture("#" + texturePost)
                .uvs(0, 4, 4, 0)
            .end()
            .face(Direction.UP)
                .texture("#" + texturePost)
                .uvs(0, 16, 4, 12)
            .end();
        return builder;
    }

}
