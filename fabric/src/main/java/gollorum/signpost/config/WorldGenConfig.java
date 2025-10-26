package gollorum.signpost.config;

import gollorum.signpost.minecraft.config.IWorldGenConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.Arrays;
import java.util.List;

public class WorldGenConfig implements ConfigData, IWorldGenConfig {
    @ConfigEntry.Gui.Tooltip
    public boolean isVillageGenerationEnabled = true;
    @ConfigEntry.Gui.Tooltip
    public boolean villagesOnlyTargetVillages = true;
    @ConfigEntry.Gui.Tooltip
    public int maxSignpostsPerVillage = 2;
    @ConfigEntry.Gui.Tooltip(count = 2)
    public List<String> allowedVillageWaystones = Arrays.asList(
        "simple_0", "simple_1", "simple_2", "detailed_0", "detailed_1"
    );
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean debugMode = false;
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.Gui.CollapsibleObject
    public Naming naming = new Naming();

    @Override
    public boolean isVillageGenerationEnabled() {
        return isVillageGenerationEnabled;
    }

    @Override
    public boolean villagesOnlyTargetVillages() {
        return villagesOnlyTargetVillages;
    }

    @Override
    public int maxSignpostsPerVillage() {
        return maxSignpostsPerVillage;
    }

    @Override
    public List<? extends String> allowedVillageWaystones() {
        return allowedVillageWaystones;
    }

    @Override
    public boolean debugMode() {
        return debugMode;
    }

    @Override
    public INaming naming() {
        return naming;
    }

    public static class Naming implements ConfigData, INaming {
        public List<String> villageNamePrefixes = Arrays.asList(
            "ak", "dev", "dol ", "ed", "il", "og", "por", "rov", "tek ", "tar", "tol ", "ves", ""
        );
        public List<String> villageNameInfixes = Arrays.asList(
            "do ", "en", "go", "na", "nah ", "ker", "ol", "ora", "ra", "rem", "ro"
        );
        public List<String> villageNamePostfixes = Arrays.asList(
            "ar", "blo", "bo", "bro", "do", "dra", "er", "ker", "lia", "tek"
        );

        @Override
        public List<? extends String> villageNamePrefixes() {
            return villageNamePrefixes;
        }

        @Override
        public List<? extends String> villageNameInfixes() {
            return villageNameInfixes;
        }

        @Override
        public List<? extends String> villageNamePostfixes() {
            return villageNamePostfixes;
        }
    }
}
