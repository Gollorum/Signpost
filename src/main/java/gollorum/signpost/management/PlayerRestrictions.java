package gollorum.signpost.management;

import gollorum.signpost.util.MyBlockPosSet;

public class PlayerRestrictions {
    public final MyBlockPosSet discoveredWastones;
    public int remainingWaystones;
    public int remainingSignposts;
    public PlayerRestrictions(MyBlockPosSet discoveredWastones, int remainingWaystones, int remainingSignposts) {
        this.discoveredWastones = discoveredWastones;
        this.remainingWaystones = remainingWaystones;
        this.remainingSignposts = remainingSignposts;
    }

    public PlayerRestrictions() {
        this(
            new MyBlockPosSet(),
            ClientConfigStorage.INSTANCE.getMaxWaystones(),
            ClientConfigStorage.INSTANCE.getMaxSignposts()
        );
    }
}

