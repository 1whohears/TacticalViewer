package com.onewhohears.tacview.forge;

import com.onewhohears.tacview.TacViewMod;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TacViewMod.MOD_ID)
public final class TacViewModForge {
    public TacViewModForge(FMLJavaModLoadingContext loadingContext) {
        EventBuses.registerModEventBus(TacViewMod.MOD_ID, loadingContext.getModEventBus());

        TacViewMod.init();
    }
}
