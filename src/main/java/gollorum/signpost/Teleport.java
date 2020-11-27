package gollorum.signpost;

import gollorum.signpost.utils.TileEntityUtils;
import gollorum.signpost.utils.WaystoneLocationData;
import gollorum.signpost.utils.math.Angle;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;

import java.util.Optional;
import java.util.UUID;

public class Teleport {

    public static void toWaystone(UUID waystoneId, PlayerEntity player){
        assert Signpost.getServerType().isServer;
        WaystoneLocationData waystoneData = WaystoneLibrary.getInstance().getLocationData(waystoneId);
        waystoneData.blockLocation.world.mapLeft(Optional::of)
            .leftOr(i -> TileEntityUtils.findWorld(i, false))
        .ifPresent(world -> {
            if(!(world instanceof ServerWorld)) return;
            Vector3 location = waystoneData.spawnPosition;
            Vector3 diff = Vector3.fromBlockPos(waystoneData.blockLocation.blockPos).add(new Vector3(0.5f, 0.5f, 0.5f))
                .subtract(location.withY(y -> y + player.getEyeHeight()));
            Angle yaw = Angle.between(
                0, 1,
                diff.x, diff.z
            );
            Angle pitch = Angle.fromRadians((float) (Math.PI / 2 + Math.atan(Math.sqrt(diff.x * diff.x + diff.z * diff.z) / diff.y)));
            if(!player.dimension.equals(world.dimension.getType()))
                player.changeDimension(world.dimension.getType(), new ITeleporter() {});
            player.rotationYaw = yaw.degrees();
            player.rotationPitch = pitch.degrees();
            player.setPositionAndUpdate(location.x, location.y, location.z);
        });
    }

}
