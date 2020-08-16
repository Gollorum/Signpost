package gollorum.signpost.minecraft.block.tiles;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gollorum.signpost.Signpost;
import gollorum.signpost.minecraft.block.Post;
import gollorum.signpost.networking.PacketHandler;
import gollorum.signpost.signtypes.PostModel;
import gollorum.signpost.signtypes.SmallWideSign;
import gollorum.signpost.utils.BlockPart;
import gollorum.signpost.utils.BlockPartInstance;
import gollorum.signpost.utils.BlockPartMetadata;
import gollorum.signpost.utils.TileEntityUtils;
import gollorum.signpost.utils.math.geometry.Ray;
import gollorum.signpost.utils.math.geometry.Vector3;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PostTile extends TileEntity {

    public static final String REGISTRY_NAME = "post";

    public static final TileEntityType<PostTile> type = TileEntityType.Builder.create(PostTile::new, Post.ALL).build(null);

    private final Map<UUID, BlockPartInstance> parts = new ConcurrentHashMap<>();
    public static final Set<BlockPartMetadata<?>> partsMetadata = new ConcurrentSet<>();
    static {
        partsMetadata.add(PostModel.METADATA);
        partsMetadata.add(SmallWideSign.METADATA);
    }

    public static class TraceResult {
        public final BlockPart part;
        public final UUID id;
        public TraceResult(BlockPart part, UUID id) {
            this.part = part;
            this.id = id;
        }
    }

    public PostTile() {
        super(type);
    }

    public UUID addPart(BlockPartInstance part){ return addPart(UUID.randomUUID(), part); }

    public UUID addPart(UUID identifier, BlockPartInstance part){
        parts.put(identifier, part);
        if(hasWorld() && !world.isRemote) sendToTracing(identifier, () -> new PartAddedEvent.Packet(
            new TilePartInfo(this, identifier),
            part.blockPart.getMeta().identifier,
            part.blockPart.write(),
            part.offset
        ));
        return identifier;
    }

    public void removePart(UUID id) {
        parts.remove(id);
        if(!world.isRemote) sendToTracing(id, () -> new PartRemovedEvent.Packet(new TilePartInfo(this, id)));
    }

    public Collection<BlockPartInstance> getParts(){ return parts.values(); }

    public VoxelShape getBounds(){
        return parts.values().stream().map(t -> t
            .blockPart.getIntersection()
            .getBounds()
            .offset(t.offset)
            .asMinecraftBB()
        ).map(VoxelShapes::create)
        .reduce((b1, b2) -> VoxelShapes.combine(b1, b2, IBooleanFunction.OR)).orElse(VoxelShapes.empty());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        VoxelShape shape = getBounds();
        return shape.isEmpty()
            ? new AxisAlignedBB(pos)
            : shape.getBoundingBox().offset(pos);
    }

    public Optional<TraceResult> trace(PlayerEntity player){
        Vec3d head = player.getPositionVector();
        head = head.add(0, player.getEyeHeight(), 0);
        if (player.isCrouching())
            head = head.subtract(0, 0.08, 0);
        Vec3d look = player.getLookVec();
        Ray ray = new Ray(Vector3.fromVec3d(head).subtract(Vector3.fromBlockPos(pos)), Vector3.fromVec3d(look));

        Optional<Tuple<UUID, Float>> closestTrace = Optional.empty();
        for(Map.Entry<UUID, BlockPartInstance> t : parts.entrySet()){
            Optional<Float> now = t.getValue().blockPart.intersectWith(ray, t.getValue().offset);
            if(now.isPresent() && (!closestTrace.isPresent() || closestTrace.get().getB() > now.get()))
                closestTrace = Optional.of(new Tuple<>(t.getKey(), now.get()));
        }
        return closestTrace.map(Tuple::getA).map(id -> new TraceResult(parts.get(id).blockPart, id));
    }
    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        writeSelf(compound);
        return compound;
    }

    private void writeSelf(CompoundNBT compound){
        for(Map.Entry<BlockPartMetadata, List<Map.Entry<UUID, BlockPartInstance>>> entry: parts.entrySet().stream()
            .collect(Collectors.groupingBy(p -> p.getValue().blockPart.getMeta())).entrySet()
        ){
            List<Map.Entry<UUID, BlockPartInstance>> instances = entry.getValue();
            compound.putInt(entry.getKey().identifier, instances.size());
            for(int i=0; i<instances.size(); i++){
                Map.Entry<UUID, BlockPartInstance> e = instances.get(i);
                BlockPartInstance instance = e.getValue();
                CompoundNBT subComp = instance.blockPart.write();
                Vector3.SERIALIZER.writeTo(instance.offset, subComp, "offset_");
                compound.put(entry.getKey().identifier + "_" + i, subComp);
                subComp.putUniqueId("partId", e.getKey());
            }
        }
    }

    @Override
    public void read(CompoundNBT compound) {
        super.read(compound);
        readSelf(compound);
    }

    private void readSelf(CompoundNBT compound){
        parts.clear();
        for(BlockPartMetadata<?> meta : partsMetadata){
            for(int i = 0; i < compound.getInt(meta.identifier); i++){
                CompoundNBT comp = compound.getCompound(meta.identifier + "_" + i);
                addPart(comp.getUniqueId("partId"), new BlockPartInstance(
                    meta.read(comp),
                    Vector3.SERIALIZER.read(comp, "offset_")
                ));
            }
        }
        if(parts.isEmpty()) addPart(
            new BlockPartInstance(new PostModel(Post.ModelType.Stone.postLocation), Vector3.ZERO)
        );
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT ret = super.getUpdateTag();
        writeSelf(ret);
        return ret;
    }

    @Override
    public void handleUpdateTag(CompoundNBT compound) {
        super.handleUpdateTag(compound);
        readSelf(compound);
    }

    public void notifyMutation(UUID part, CompoundNBT data) {
        sendToTracing(part,
            () -> new PostTile.PartMutatedEvent.Packet(
                new PostTile.TilePartInfo(this, part),
                data
            )
        );
    }

    public <T> void sendToTracing(UUID part, Supplier<T> t) {
        if(!hasWorld()) Signpost.LOGGER.warn("No world to notify mutation");
        else PacketHandler.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), t.get());
    }

    public static class TilePartInfo {
        public final int dimensionId;
        public final BlockPos pos;
        public final UUID identifier;

        public TilePartInfo(TileEntity tile, UUID identifier) {
            this.dimensionId = tile.getWorld().dimension.getType().getId();
            this.pos = tile.getPos();
            this.identifier = identifier;
        }

        public TilePartInfo(int dimensionId, BlockPos pos, UUID identifier) {
            this.dimensionId = dimensionId;
            this.pos = pos;
            this.identifier = identifier;
        }

        public void writeTo(PacketBuffer buffer) {
            buffer.writeInt(dimensionId);
            buffer.writeLong(pos.toLong());
            buffer.writeString(identifier.toString());
        }

        public static TilePartInfo readFrom(PacketBuffer buffer) {
            return new TilePartInfo(
                buffer.readInt(),
                BlockPos.fromLong(buffer.readLong()),
                UUID.fromString(buffer.readString())
            );
        }
    }

    public static class PartAddedEvent implements PacketHandler.Event<PartAddedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public final String partMetaIdentifier;
            public final CompoundNBT partData;
            public final Vector3 offset;

            public Packet(TilePartInfo info, String partMetaIdentifier, CompoundNBT partData, Vector3 offset) {
                this.info = info;
                this.partMetaIdentifier = partMetaIdentifier;
                this.partData = partData;
                this.offset = offset;
            }

            public Packet(TilePartInfo info, BlockPartInstance instance) {
                this.info = info;
                this.partMetaIdentifier = instance.blockPart.getMeta().identifier;
                this.partData = instance.blockPart.write();
                this.offset = instance.offset;
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            message.info.writeTo(buffer);
            buffer.writeString(message.partMetaIdentifier);
            buffer.writeString(message.partData.getString());
            Vector3.SERIALIZER.writeTo(message.offset, buffer);
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            try {
                return new Packet(
                    TilePartInfo.readFrom(buffer),
                    buffer.readString(),
                    JsonToNBT.getTagFromJson(buffer.readString()),
                    Vector3.SERIALIZER.readFrom(buffer)
                );
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                throw new RuntimeException("An exception occurred in PostTile Packet NBT decoding");
            }
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            TileEntityUtils.findTileEntityClient(
                DimensionType.getById(message.info.dimensionId),
                message.info.pos,
                PostTile.class
            ).ifPresent(tile -> {
                Optional<BlockPartMetadata<?>> meta = partsMetadata.stream().filter(m -> m.identifier.equals(message.partMetaIdentifier)).findFirst();
                if(meta.isPresent()) {
                    tile.addPart(message.info.identifier,
                        new BlockPartInstance(meta.get().read(message.partData), message.offset));
                } else {
                    Signpost.LOGGER.warn("Could not find meta for part "+message.partMetaIdentifier);
                }
            });
        }

    }

    public static class PartRemovedEvent implements PacketHandler.Event<PartRemovedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public Packet(TilePartInfo info) {
                this.info = info;
            }
            public Packet(int dimensionId, BlockPos pos, UUID identifier) {
                this(new TilePartInfo(dimensionId, pos, identifier));
            }
        }

        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) { message.info.writeTo(buffer); }

        @Override
        public Packet decode(PacketBuffer buffer) {
            return new Packet(TilePartInfo.readFrom(buffer));
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            TileEntityUtils.findTileEntityClient(
                DimensionType.getById(message.info.dimensionId),
                message.info.pos,
                PostTile.class
            ).ifPresent(tile -> tile.removePart(message.info.identifier));
        }
    }

    public static class PartMutatedEvent implements PacketHandler.Event<PartMutatedEvent.Packet> {

        public static class Packet {
            public final TilePartInfo info;
            public final CompoundNBT data;

            public Packet(TilePartInfo info, CompoundNBT data) {
                this.info = info;
                this.data = data;
            }
        }
        @Override
        public Class<Packet> getMessageClass() { return Packet.class; }

        @Override
        public void encode(Packet message, PacketBuffer buffer) {
            message.info.writeTo(buffer);
            buffer.writeString(message.data.toString());
        }

        @Override
        public Packet decode(PacketBuffer buffer) {
            try {
                return new Packet(
                    TilePartInfo.readFrom(buffer),
                    JsonToNBT.getTagFromJson(buffer.readString())
                );
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void handle(Packet message, Supplier<NetworkEvent.Context> context) {
            TileEntityUtils.findTileEntityClient(
                DimensionType.getById(message.info.dimensionId),
                message.info.pos,
                PostTile.class
            ).ifPresent(tile ->
                tile.parts
                    .get(message.info.identifier)
                    .blockPart
                    .readMutationUpdate(message.data, tile));
        }
    }
}
