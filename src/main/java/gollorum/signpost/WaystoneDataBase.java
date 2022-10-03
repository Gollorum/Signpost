package gollorum.signpost;

import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.WorldLocation;

public interface WaystoneDataBase {

    String name();
    WaystoneLocationData loc();

    WaystoneHandle handle();

}
