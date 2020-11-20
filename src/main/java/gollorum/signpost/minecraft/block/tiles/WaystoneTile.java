package gollorum.signpost.minecraft.block.tiles;

import gollorum.signpost.WaystoneLibrary;
import gollorum.signpost.minecraft.block.Waystone;
import gollorum.signpost.utils.Delay;
import gollorum.signpost.utils.WaystoneData;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

public class WaystoneTile extends TileEntity {

    public static final String REGISTRY_NAME = "waystone";

    public static final TileEntityType<WaystoneTile> type = TileEntityType.Builder.create(WaystoneTile::new, Waystone.INSTANCE).build(null);

    public WaystoneTile() { super(type); }

    public void update(WaystoneData data) {
        Delay.until(this::hasWorld,
            () -> {
                WaystoneLibrary.getInstance().update(
                    data.name,
                    new WaystoneLocationData(
                        WorldLocation.from(this).get(),
                        data.localSpawnLocation
                    )
                );
            }
        );
    }

    @Override
    public void remove() {
        super.remove();
    }

}
