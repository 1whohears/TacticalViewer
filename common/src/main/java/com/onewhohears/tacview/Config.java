package com.onewhohears.tacview;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Config {
    public static class Client {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> showValues;
        public Client(ForgeConfigSpec.Builder builder) {
            showValues = builder.defineList("showValues",
                    List.of("health"),
                    entry -> true);
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
