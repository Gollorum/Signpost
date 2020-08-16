package gollorum.signpost.utils;

import gollorum.signpost.utils.math.geometry.Vector3;

public class BlockPartInstance {

    public final BlockPart blockPart;
    public final Vector3 offset;

    public BlockPartInstance(BlockPart blockPart, Vector3 offset) {
        this.blockPart = blockPart;
        this.offset = offset;
    }
}
