package com.onewhohears.tacview;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Config {
    public static class Client {
        public final ForgeConfigSpec.IntValue maxHeightMapTiles;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> hideValues;
        public Client(ForgeConfigSpec.Builder builder) {
            hideValues = builder.defineList("hideValues",
                    List.of(), entry -> true);
            maxHeightMapTiles = builder.comment("The max number of tiles the height map will generate and render." +
                            " If the total area of the replay exceeds this value, then the height map will be generated" +
                            " at a lower level of detail." +
                            " If too big, Distant Horizons may poop itself and not give any height map data." +
                            " If too small, the inaccuracies may cause entities to sometimes render under the height map.")
                    .defineInRange("maxHeightMapTiles", 16384, 1, Integer.MAX_VALUE);
        }
    }

    public static final ForgeConfigSpec clientSpec;
    public static final Config.Client CLIENT;

    static {
        final Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder()
                .configure(Config.Client::new);
        clientSpec = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
    }
}
