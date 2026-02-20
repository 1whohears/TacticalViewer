package com.onewhohears.tacview.fabric.client;

import com.onewhohears.tacview.client.renderer.TVEntityRenderers;
import net.fabricmc.api.ClientModInitializer;

public final class TacViewModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TVEntityRenderers.register();
    }
}
