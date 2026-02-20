package com.onewhohears.tacview.common.event;

import com.onewhohears.tacview.common.core.SessionManager;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;

public class TVCommonEventHandlers {

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(TVCommonEventHandlers::onServerLevelPost);
    }

    private static void onServerLevelPost(ServerLevel level) {
        SessionManager.get().tickRecord(level);
    }

}
