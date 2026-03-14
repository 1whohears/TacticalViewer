package com.onewhohears.tacview.client.overlay;

import com.onewhohears.onewholibs.util.UtilMCText;
import com.onewhohears.tacview.client.core.ClientPlayback;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlaybackInfoOverlay {

    public static final Style GREEN = Style.EMPTY.withColor(0x00ff00);

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

        String currentSeconds = String.format("%.2f", (tick-startTick)/20d);
        String lengthSeconds = String.format("%.2f", length/20d);
        String titleStr = sessionId+" "+currentSeconds+"/"+lengthSeconds+" "+(paused?"Paused":"");

        Component title = UtilMCText.literal(titleStr).withStyle(GREEN);
        gui.drawString(m.font, title, 0, 0, 10);

        List<Component> entityInfos = playback.getOverlayEntityInfo();
        for (int i = 0; i < entityInfos.size(); ++i) {
            gui.drawString(m.font, entityInfos.get(i), 0, 10*i+10, 10);
        }
    }

    public static void setOverlayTarget(@Nullable ClientPlayback pb) {
        playback = pb;
    }

}
