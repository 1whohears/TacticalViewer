package com.onewhohears.tacview.fabric;

import com.onewhohears.tacview.Config;
import com.onewhohears.tacview.TacViewMod;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraftforge.fml.config.ModConfig;

import static com.onewhohears.tacview.TacViewMod.MOD_ID;

public final class TacViewModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TacViewMod.init();
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.CLIENT, Config.clientSpec);
    }
}
