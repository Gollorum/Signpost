package gollorum.signpost.blockpartdata.types;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import gollorum.signpost.WaystoneHandle;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.minecraft.block.PostBlock;
import gollorum.signpost.minecraft.utils.CoordinatesUtil;
import gollorum.signpost.minecraft.utils.Texture;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.AngleProvider;
import gollorum.signpost.utils.NameProvider;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Matrix4x4;
import gollorum.signpost.utils.math.geometry.TransformedBox;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class LargeSignBlockPart extends SignBlockPart<LargeSignBlockPart> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -14, 2),
        new Vector3(12, -2, 3)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<LargeSignBlockPart> METADATA = new BlockPartMetadata<>(
        "large_sign",
        version -> RecordCodecBuilder.mapCodec(i -> i.group(
            CoreData.codec(version).fieldOf("CoreData").forGetter(sign -> sign.coreData),
            NameProvider.CODEC.fieldOf("Text0").forGetter(sign -> sign.text[0]),
            NameProvider.CODEC.fieldOf("Text1").forGetter(sign -> sign.text[1]),
            NameProvider.CODEC.fieldOf("Text2").forGetter(sign -> sign.text[2]),
            NameProvider.CODEC.fieldOf("Text3").forGetter(sign -> sign.text[3])
        ).apply(i, LargeSignBlockPart::new)),
        StreamCodec.composite(
            CoreData.STREAM_CODEC, sign -> sign.coreData,
            NameProvider.STREAM_CODEC, sign -> sign.text[0],
            NameProvider.STREAM_CODEC, sign -> sign.text[1],
            NameProvider.STREAM_CODEC, sign -> sign.text[2],
            NameProvider.STREAM_CODEC, sign -> sign.text[3],
            LargeSignBlockPart::new
        ),
        LargeSignBlockPart.class);

    private NameProvider[] text;

    public LargeSignBlockPart(
        CoreData coreData,
        NameProvider text0,
        NameProvider text1,
        NameProvider text2,
        NameProvider text3
    ) {
        super(coreData);
        this.text = new NameProvider[]{text0, text1, text2, text3};
    }

    public LargeSignBlockPart(
        CoreData coreData,
        NameProvider[] text
    ) {
        super(coreData);
        assert text.length == 4;
        this.text = text;
    }

    public LargeSignBlockPart(
        AngleProvider angle,
        NameProvider[] text,
        boolean flip,
        Texture mainTexture,
        Texture secondaryTexture,
        Optional<Overlay> overlay,
        int color,
        Optional<WaystoneHandle> destination,
        Optional<ItemStack> itemToDropOnBreak,
        ResourceKey<PostBlock.ModelType> modelType,
        boolean isLocked,
        boolean isMarkedForGeneration
    ) { this(
        new CoreData(angle, flip, mainTexture, secondaryTexture, overlay,
            color, destination, modelType, itemToDropOnBreak, isLocked, isMarkedForGeneration),
        text
    ); }

    public void setText(NameProvider[] text) {
        this.text = text;
    }

    public NameProvider[] getText() { return text; }

    @Override
    protected NameProvider[] getNameProviders() {
        return text;
    }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, coreData.angleProvider.get());
        if(coreData.flip) transformedBounds = transformedBounds.scale(new Vector3(1, 1, -1));
    }

    @Override
    public LargeSignBlockPart copy() {
        return new LargeSignBlockPart(coreData.copy(), text);
    }

    @Override
    public BlockPartMetadata<LargeSignBlockPart> getMeta() {
        return METADATA;
    }

}
