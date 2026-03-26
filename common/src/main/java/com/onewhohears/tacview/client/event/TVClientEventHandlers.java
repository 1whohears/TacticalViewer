package com.onewhohears.tacview.client.event;

import com.onewhohears.tacview.client.core.TVClientManager;
import com.onewhohears.tacview.client.overlay.PlaybackInfoOverlay;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class TVClientEventHandlers {

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(TVClientEventHandlers::onClientTick);
        ClientGuiEvent.RENDER_HUD.register(TVClientEventHandlers::onRenderHud);
        // TODO client DH cache periodically and on new level loads
    }

    private static void onRenderHud(GuiGraphics gui, float partialTick) {
        PlaybackInfoOverlay.render(gui, partialTick);
    }

    public static void onClientTick(Minecraft minecraft) {
        TVClientManager.get().tick();
    }
}
