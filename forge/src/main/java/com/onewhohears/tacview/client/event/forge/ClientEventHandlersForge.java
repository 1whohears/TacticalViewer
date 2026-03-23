package com.onewhohears.tacview.client.event.forge;

import com.onewhohears.tacview.TacViewMod;
import com.onewhohears.tacview.client.core.TVClientManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TacViewMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandlersForge {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cameraSetup(ViewportEvent.ComputeCameraAngles event) {
        TVClientManager.get().playerLookAtTrackedEntity(event.getCamera().getPosition(), (xRot, yRot) -> {
            Minecraft m = Minecraft.getInstance();
            m.player.setXRot(xRot);
            m.player.setYRot(yRot);
            event.setPitch(xRot);
            event.setYaw(yRot);
        });
    }
}
