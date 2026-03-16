package com.onewhohears.tacview;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Config {
    public static class Client {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> hideValues;
        public Client(ForgeConfigSpec.Builder builder) {
            hideValues = builder.defineList("hideValues",
                    List.of(), entry -> true);
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
