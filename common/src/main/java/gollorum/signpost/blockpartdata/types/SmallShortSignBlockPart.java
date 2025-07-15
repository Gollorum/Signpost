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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class SmallShortSignBlockPart extends SignBlockPart<SmallShortSignBlockPart> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(2, -11, 0.5f),
        new Vector3(18, -5, -0.5f)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<SmallShortSignBlockPart> METADATA = new BlockPartMetadata<>(
        "small_short_sign",
        RecordCodecBuilder.mapCodec(i -> i.group(
            CoreData.CODEC.fieldOf("CoreData").forGetter(sign -> sign.coreData),
            NameProvider.CODEC.fieldOf("Text").forGetter(sign -> sign.text)
        ).apply(i, SmallShortSignBlockPart::new)),
        StreamCodec.composite(
            CoreData.STREAM_CODEC, sign -> sign.coreData,
            NameProvider.STREAM_CODEC, sign -> sign.text,
            SmallShortSignBlockPart::new
        ),
        SmallShortSignBlockPart.class
    );

    private NameProvider text;

    public SmallShortSignBlockPart(
        CoreData coreData,
        NameProvider text
    ){
        super(coreData);
        this.text = text;
    }

    public SmallShortSignBlockPart(
        AngleProvider angle,
        NameProvider text,
        boolean flip,
        Texture mainTexture,
        Texture secondaryTexture,
        Optional<Overlay> overlay,
        int color,
        Optional<WaystoneHandle> destination,
        ItemStack itemToDropOnBreak,
        PostBlock.ModelType modelType,
        boolean isLocked,
        boolean isMarkedForGeneration
    ) { this(
        new CoreData(angle, flip, mainTexture, secondaryTexture, overlay,
            color, destination, modelType, itemToDropOnBreak, isLocked, isMarkedForGeneration),
        text
    ); }

    public void setText(NameProvider text) { this.text = text; }

    public NameProvider getText() { return text; }

    @Override
    protected NameProvider[] getNameProviders() {
        return new NameProvider[]{text};
    }

    @Override
    protected void regenerateTransformedBox() {
        transformedBounds = new TransformedBox(LOCAL_BOUNDS).rotateAlong(Matrix4x4.Axis.Y, coreData.angleProvider.get());
    }

    @Override
    public SmallShortSignBlockPart copy() {
        return new SmallShortSignBlockPart(coreData.copy(), text);
    }

    @Override
    public BlockPartMetadata<SmallShortSignBlockPart> getMeta() {
        return METADATA;
    }

}
