package com.onewhohears.tacview.client.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

public class ClientPlayback {

    private final TacViewEntity parent;

    public ClientPlayback(@NotNull TacViewEntity parent) {
        this.parent = parent;
    }

    /**
     * CLIENT SIDE ONLY
     */
    public void render(float yaw, float partialTick, PoseStack stack, MultiBufferSource buffer, int packedLight) {
        String sessionId = parent.getSessionId();
        RecordingSession session = SessionManager.get().getSession(sessionId);
        if (session == null) {
            TVClientManager.get().requestRecordingSessionFromServer(sessionId);
            return;
        }

        float width = parent.getWidth();
        float height = parent.getHeight();
        long tick = parent.getPlaybackTick();
        boolean paused = parent.isPaused();
        int tickRate = parent.getTickRate();

        stack.pushPose();
        Minecraft m = Minecraft.getInstance();



        stack.popPose();
    }

}
