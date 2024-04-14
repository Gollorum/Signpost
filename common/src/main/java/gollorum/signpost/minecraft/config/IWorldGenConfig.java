package gollorum.signpost.minecraft.config;

import java.util.List;

public interface IWorldGenConfig {

	boolean isVillageGenerationEnabled();
	boolean villagesOnlyTargetVillages();
	int maxSignpostsPerVillage();
	List<? extends String> allowedVillageWaystones();
	boolean debugMode();
	INaming naming();

	interface INaming {

        List<? extends String> villageNamePrefixes();
        List<? extends String> villageNameInfixes();
        List<? extends String> villageNamePostfixes();

    }
}