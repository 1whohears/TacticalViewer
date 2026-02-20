package com.onewhohears.tacview.client.event;

import com.onewhohears.tacview.client.core.TVClientManager;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

public class TVClientEventHandlers {

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(TVClientEventHandlers::onClientTick);
    }

    public static void onClientTick(Minecraft minecraft) {
        TVClientManager.get().tick();
    }
}
