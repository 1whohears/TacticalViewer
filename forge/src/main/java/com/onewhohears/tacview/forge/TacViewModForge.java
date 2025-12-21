package com.onewhohears.tacview.forge;

import com.onewhohears.tacview.TacViewMod;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TacViewMod.MOD_ID)
public final class TacViewModForge {
    public TacViewModForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(TacViewMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        TacViewMod.init();
    }
}
