package com.onewhohears.tacview.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

public class TVClientManager {

    public void tick() {

    }

    public void render(TacViewEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffer, int packedLight) {
        stack.pushPose();
        Minecraft m = Minecraft.getInstance();



        stack.popPose();
    }

    private static TVClientManager INSTANCE = null;

    public static TVClientManager get() {
        if (INSTANCE == null) INSTANCE = new TVClientManager();
        return INSTANCE;
    }

    private TVClientManager() {}

}
