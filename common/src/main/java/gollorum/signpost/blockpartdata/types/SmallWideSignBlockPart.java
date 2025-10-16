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

public class SmallWideSignBlockPart extends SignBlockPart<SmallWideSignBlockPart> {

    private static final AABB LOCAL_BOUNDS = new AABB(
        new Vector3(-9, -11, 2),
        new Vector3(16, -5, 3)
    ).map(CoordinatesUtil::voxelToLocal);

    public static final BlockPartMetadata<SmallWideSignBlockPart> METADATA = new BlockPartMetadata<>(
        "small_wide_sign",
        version -> RecordCodecBuilder.mapCodec(i -> i.group(
            CoreData.codec(version).fieldOf("CoreData").forGetter(sign -> sign.coreData),
            NameProvider.CODEC.fieldOf("Text").forGetter(SmallWideSignBlockPart::getText)
        ).apply(i, SmallWideSignBlockPart::new)),
        StreamCodec.composite(
            CoreData.STREAM_CODEC, sign -> sign.coreData,
            NameProvider.STREAM_CODEC, SmallWideSignBlockPart::getText,
            SmallWideSignBlockPart::new
        ),
        SmallWideSignBlockPart.class
    );

    private NameProvider text;

    public SmallWideSignBlockPart(
        CoreData coreData,
        NameProvider text
    ){
        super(coreData);
        this.text = text;
    }

    public SmallWideSignBlockPart(
        AngleProvider angle,
        NameProvider text,
        boolean flip,
        Texture mainTexture,
        Texture secondaryTexture,
        Optional<Overlay> overlay,
        int color,
        Optional<WaystoneHandle> destination,
        Optional<ItemStack> itemToDropOnBreak,
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
        if(coreData.flip) transformedBounds = transformedBounds.scale(new Vector3(1, 1, -1));
    }

    @Override
    public SmallWideSignBlockPart copy() {
        return new SmallWideSignBlockPart(coreData.copy(), text);
    }

    @Override
    public BlockPartMetadata<SmallWideSignBlockPart> getMeta() {
        return METADATA;
    }

}
