package com.onewhohears.tacview.client.overlay;

import com.onewhohears.tacview.client.core.ClientPlayback;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

public class PlaybackInfoOverlay {

    @Nullable private static ClientPlayback playback;

    public static void render(GuiGraphics gui, float partialTick) {
        if (playback == null) return;
        String sessionId = playback.getParent().getSessionId();
        RecordingSession session = SessionManager.get().getSession(sessionId);
        if (session == null || !session.isRecordingComplete()) return;
        Minecraft m = Minecraft.getInstance();

        boolean paused = playback.getParent().isPaused();
        int length = session.getLength();
        long startTick = session.getSessionStartTime();
        long tick = playback.getParent().getPlaybackTick();

        gui.drawString(m.font, sessionId, 0, 0, 10);
    }

    public static void setOverlayTarget(@Nullable ClientPlayback pb) {
        playback = pb;
    }

}
