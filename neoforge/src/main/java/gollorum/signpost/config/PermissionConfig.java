package gollorum.signpost.config;

import gollorum.signpost.minecraft.config.IPermissionConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class PermissionConfig implements IPermissionConfig {

    private final ModConfigSpec.ConfigValue<Integer> editLockedWaystoneCommandPermissionLevel;
    private final ModConfigSpec.ConfigValue<Integer> editLockedSignCommandPermissionLevel;
    private final ModConfigSpec.ConfigValue<Integer> teleportPermissionLevel;
    private final ModConfigSpec.ConfigValue<Integer> discoverPermissionLevel;
    private final ModConfigSpec.ConfigValue<Integer> listPermissionLevel;

    private final ModConfigSpec.ConfigValue<Integer> pickUnownedWaystonePermissionLevel;

    @Override
    public int editLockedWaystoneCommandPermissionLevel() { return editLockedWaystoneCommandPermissionLevel.get(); }

    @Override
    public int editLockedSignCommandPermissionLevel() { return editLockedSignCommandPermissionLevel.get(); }

    @Override
    public int teleportPermissionLevel() { return teleportPermissionLevel.get(); }

    @Override
    public int discoverPermissionLevel() { return discoverPermissionLevel.get(); }

    @Override
    public int listPermissionLevel() { return listPermissionLevel.get(); }

    @Override
    public int pickUnownedWaystonePermissionLevel() { return pickUnownedWaystonePermissionLevel.get(); }

    public PermissionConfig(ModConfigSpec.Builder builder) {
        teleportPermissionLevel = builder.define("teleport_command_permission_level", 3);
        discoverPermissionLevel = builder.define("discover_command_permission_level", 3);
        listPermissionLevel = builder.define("list_command_permission_level", 3);

        pickUnownedWaystonePermissionLevel = builder
            .comment("Defines who (except the owner) can move a waystone by picking it / destroying it with silk touch")
            .define("pick_unowned_waystone_permission_level", 3);

        editLockedWaystoneCommandPermissionLevel = builder.define("edit_locked_waystones_permission_level", 3);
        editLockedSignCommandPermissionLevel = builder.define("edit_locked_signs_permission_level", 3);

    }
}
