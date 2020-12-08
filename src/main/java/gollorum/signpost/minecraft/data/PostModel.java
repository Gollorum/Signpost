package gollorum.signpost.minecraft.data;

import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.minecraft.block.Waystone;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PostModel extends BlockModelProvider {

    private static class FaceData {
        public final Direction direction;
        public final String texture;

        private FaceData(Direction direction, String texture) {
            this.direction = direction;
            this.texture = texture;
        }

        public static FaceData[] uniform(String texture, Direction... directions) {
            return Arrays.stream(directions).map(d -> new FaceData(d, texture)).toArray(FaceData[]::new);
        }

        public static FaceData[] all(String texture, Predicate<Direction> where) {
            return Arrays.stream(Direction.values())
                .filter(where)
                .map(d -> new FaceData(d, texture))
                .toArray(FaceData[]::new);
        }
    }

    private static final String texturePost = "post";
    private static final String textureSign = "texture";
    private static final String secondaryTexture = "secondary_texture";

    private static final ResourceLocation previewLocation = new ResourceLocation(Signpost.MOD_ID, "post_preview");
    private static final ResourceLocation wideLocation = new ResourceLocation(Signpost.MOD_ID, "small_wide_sign");
    private static final ResourceLocation shortLocation = new ResourceLocation(Signpost.MOD_ID, "small_short_sign");
    private static final ResourceLocation largeLocation = new ResourceLocation(Signpost.MOD_ID, "large_sign");

    public final Map<Post.Info, BlockModelBuilder> allModels;
    public final BlockModelBuilder waystoneModel;

    private final BlockModelBuilder previewModel;

    private static final int textureCenterY = 8;

    public PostModel(DataGenerator generator, ExistingFileHelper fileHelper) {
        super(generator, Signpost.MOD_ID, fileHelper);
        previewModel = new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + previewLocation.getPath()), fileHelper);
        allModels = Arrays.stream(Post.All_INFOS).collect(Collectors.<Post.Info, Post.Info, BlockModelBuilder>toMap(
            i -> i,
            i -> new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + i.registryName), fileHelper)
        ));
        waystoneModel = new BlockModelBuilder(new ResourceLocation(Signpost.MOD_ID, "block/" + Waystone.REGISTRY_NAME), fileHelper);
        generator.addProvider(new Item(generator, fileHelper));
    }

    private class Item extends ItemModelProvider {

        public Item(DataGenerator generator, ExistingFileHelper existingFileHelper) {
            super(generator, Signpost.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            for (Map.Entry<Post.Info, BlockModelBuilder> entry : allModels.entrySet()) {
                getBuilder(entry.getKey().registryName).parent(entry.getValue());
            }
            getBuilder(Waystone.REGISTRY_NAME).parent(waystoneModel);
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
        makeSignAt(new Vector3(8, 12, 10.5f), 6, 8, 12, 4, true, previewBuilder);

        makePostAt(new Vector3(0, 8, 0), getBuilder("post_only"))
            .texture(texturePost, Post.ModelType.Oak.postTexture);
        makeSignAt(new Vector3(0, 0, 2.5f), 6, 8, 12, 4, true, getBuilder(wideLocation.toString()))
            .texture(textureSign, Post.ModelType.Oak.mainTexture)
            .texture(secondaryTexture, Post.ModelType.Oak.secondaryTexture);
        makeSignAt(new Vector3(0, 0, 0), 6, -2, 14, 4, false, getBuilder(shortLocation.toString()))
            .texture(textureSign, Post.ModelType.Oak.mainTexture)
            .texture(secondaryTexture, Post.ModelType.Oak.secondaryTexture);
        makeSignAt(new Vector3(0, 0, 2.5f), 12, 8, 12, 3, true, getBuilder(largeLocation.toString()))
            .texture(textureSign, Post.ModelType.Oak.mainTexture)
            .texture(secondaryTexture, Post.ModelType.Oak.secondaryTexture);

        for(Post.Info info : Post.All_INFOS) {
            getBuilder(info.registryName)
                .parent(previewModel)
                .texture("particle", info.type.postTexture)
                .texture(texturePost, info.type.postTexture)
                .texture(textureSign, info.type.mainTexture)
                .texture(secondaryTexture, info.type.secondaryTexture);
        }
        cubeAll(Waystone.REGISTRY_NAME, new ResourceLocation(Signpost.MOD_ID, "block/waystone"));
    }

    private static BlockModelBuilder makeSignAt(Vector3 center, float height, float widthLeft, float widthRight, float arrowWidth, boolean shouldAddLeftRim, BlockModelBuilder builder) {
        if(shouldAddLeftRim) makePartialCube(
            builder,
            center.add(-widthLeft - 1, -height / 2 + 1, -0.5f),
            new Vector3(1, height - 2, 1),
            15, textureCenterY - height / 2 + 1,
            FaceData.uniform(secondaryTexture, Direction.UP, Direction.DOWN, Direction.WEST, Direction.NORTH, Direction.SOUTH)
        );
        if(widthLeft + widthRight > 16){
            makeSliceWithRim(
                builder,
                center.add(-widthLeft, -height / 2, -0.5f),
                new Vector3(16, height, 1),
                1,
                0, textureCenterY - height / 2,
                true, false
            );
            makeSliceWithRim(
                builder,
                center.add(16 - widthLeft, -height / 2, -0.5f),
                new Vector3(widthLeft + widthRight - 16, height, 1),
                1,
                0, textureCenterY - height / 2,
                false, true
            );
        } else makeSliceWithRim(
            builder,
            center.add(-widthLeft, -height / 2, -0.5f),
            new Vector3(widthLeft + widthRight, height, 1),
            1,
            0, textureCenterY - height / 2,
            true, true
        );
        int stairsCount = Math.round(Math.min((height - 2) / 2, arrowWidth));
        int stairStep = Math.round((height - 2) / (2 * stairsCount));
        float stairsWidth = arrowWidth / stairsCount;
        for(int i = 0; i < stairsCount - 1; i++) {
            makeSliceWithRim(
                builder,
                center.add(widthRight + stairsWidth * i, -height / 2 + 1 + stairStep * i, -0.5f),
                new Vector3(stairsWidth, height - 2 * (1 + stairStep * i), 1),
                stairStep,
                (widthLeft + widthRight + Math.round(stairsWidth) * i) % 16, textureCenterY - height / 2 + 1 + stairStep * i,
                false, true
            );
        }
        int lastI = stairsCount - 1;
        makePartialCube(
            builder,
            center.add(widthRight + stairsWidth * lastI, -height / 2 + 1 + stairStep * lastI, -0.5f),
            new Vector3(stairsWidth, height - 2 * (1 + stairStep * lastI), 1),
            (widthLeft + widthRight + Math.round(stairsWidth) * lastI) % 16, textureCenterY - height / 2 + 1 + stairStep * lastI,
            FaceData.uniform(secondaryTexture, Direction.UP, Direction.DOWN, Direction.EAST, Direction.NORTH, Direction.SOUTH)
        );
        return builder;
    }

    private static void makeSliceWithRim(BlockModelBuilder builder, Vector3 min, Vector3 size, int rimHeight, float uMin, float vMin, boolean shouldRenderWest, boolean shouldRenderEast) {
        assert size.y > 2;
        Predicate<Direction> sideNotCulled = d ->
            (!d.equals(Direction.WEST) || shouldRenderWest) &&
            (!d.equals(Direction.EAST) || shouldRenderEast);
        makePartialCube(
            builder,
            min,
            new Vector3(size.x, rimHeight, size.z),
            uMin, vMin + size.y - rimHeight,
            FaceData.all(secondaryTexture, d -> sideNotCulled.test(d) && !d.equals(Direction.UP))
        );
        makePartialCube(
            builder,
            min.withY(y -> y + size.y - rimHeight),
            new Vector3(size.x, rimHeight, size.z),
            uMin, vMin,
            FaceData.all(secondaryTexture, d -> sideNotCulled.test(d) && !d.equals(Direction.DOWN))
        );
        makePartialCube(
            builder,
            min.withY(y -> y + rimHeight),
            new Vector3(size.x, size.y - 2 * rimHeight, size.z),
            uMin, vMin + rimHeight,
            new FaceData(Direction.SOUTH, textureSign), new FaceData(Direction.NORTH, secondaryTexture)
        );
    }

    private static void makePartialCube(BlockModelBuilder builder, Vector3 min, Vector3 size, float minU, float minV, FaceData... directions) {
        ModelBuilder<BlockModelBuilder>.ElementBuilder elementBuilder = builder.element()
            .from(min.x, min.y, min.z)
            .to(min.x + size.x, min.y + size.y, min.z + size.z);
        float maxFrontU = minU + size.x;
        float maxFrontV = minV + size.y;
        for(FaceData faceData : directions) {
            ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder faceBuilder = elementBuilder
                .face(faceData.direction)
                .texture("#" + faceData.texture);
            TextureSegment uCoords;
            TextureSegment vCoords;
            switch (faceData.direction) {
                case DOWN:
                    uCoords = new TextureSegment(minU, maxFrontU);
                    vCoords = new TextureSegment(minV - size.z, minV);
                    break;
                case UP:
                    uCoords = new TextureSegment(minU, maxFrontU);
                    vCoords = new TextureSegment(maxFrontV, maxFrontV + size.z);
                    break;
                case SOUTH:
                    uCoords = new TextureSegment(minU, maxFrontU);
                    vCoords = new TextureSegment(minV, maxFrontV);
                    break;
                case NORTH:
                    uCoords = new TextureSegment(maxFrontU, minU);
                    vCoords = new TextureSegment(minV, maxFrontV);
                    break;
                case WEST:
                    uCoords = new TextureSegment(maxFrontU, maxFrontU + size.z);
                    vCoords = new TextureSegment(minV, maxFrontV);
                    break;
                case EAST:
                    uCoords = new TextureSegment(minU - size.z, minU);
                    vCoords = new TextureSegment(minV, maxFrontV);
                    break;
                default: throw new RuntimeException("Direction " + faceData.direction + " is not supported");
            }
            faceBuilder.uvs(uCoords.from, vCoords.from, uCoords.to, vCoords.to);
        }
    }

    private static class TextureSegment {
        public final float from;
        public final float to;

        private TextureSegment(float from, float to) {
            if(!(isInTextureBounds(from) && isInTextureBounds(to))) {
                float originalFrom = from;
                float originalTo = to;
                float diff = to - from;
                from = from % 16;
                if(from < 0) from += 16;
                if(from == 0 && diff < 0) from = 16;
                to = from + diff;
                if(!isInTextureBounds(to))
                    throw new RuntimeException("The coordinates cannot be clamped; they cut the boundary: (" + originalFrom +"|" + originalTo + ")");
            }
            this.from = from;
            this.to = to;
        }

        private static boolean isInTextureBounds(float i) {
            return i >= 0 && i <= 16;
        }

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
