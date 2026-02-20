package com.onewhohears.tacview.forge;

import com.onewhohears.tacview.TacViewMod;
import com.onewhohears.tacview.client.renderer.TVEntityRenderers;
import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.utils.Env;
import net.minecraftforge.data.loading.DatagenModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TacViewMod.MOD_ID)
public final class TacViewModForge {
    public TacViewModForge(FMLJavaModLoadingContext loadingContext) {
        EventBuses.registerModEventBus(TacViewMod.MOD_ID, loadingContext.getModEventBus());

        TacViewMod.init();

        if (Platform.getEnvironment() == Env.CLIENT && !DatagenModLoader.isRunningDataGen()) {
            TVEntityRenderers.register();
        }
    }
}
