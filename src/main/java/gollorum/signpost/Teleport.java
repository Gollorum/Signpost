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

    public static void toWaystone(WaystoneHandle waystone, PlayerEntity player){
        assert Signpost.getServerType().isServer;
        WaystoneLocationData waystoneData = WaystoneLibrary.getInstance().getLocationData(waystone);
        waystoneData.block.world.mapLeft(Optional::of)
            .leftOr(i -> TileEntityUtils.findWorld(i, false))
        .ifPresent(unspecificWorld -> {
            if(!(unspecificWorld instanceof ServerWorld)) return;
            ServerWorld world = (ServerWorld) unspecificWorld;
            Vector3 location = waystoneData.spawn;
            Vector3 diff = Vector3.fromBlockPos(waystoneData.block.blockPos).add(new Vector3(0.5f, 0.5f, 0.5f))
                .subtract(location.withY(y -> y + player.getEyeHeight()));
            Angle yaw = Angle.between(
                0, 1,
                diff.x, diff.z
            );
            Angle pitch = Angle.fromRadians((float) (Math.PI / 2 + Math.atan(Math.sqrt(diff.x * diff.x + diff.z * diff.z) / diff.y)));
            if(!player.world.getDimensionType().equals(world.getDimensionType()))
                player.changeDimension(world, new ITeleporter() {});
            player.rotationYaw = yaw.degrees();
            player.rotationPitch = pitch.degrees();
            player.setPositionAndUpdate(location.x, location.y, location.z);
        });
    }

}
