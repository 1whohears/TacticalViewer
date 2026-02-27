package com.onewhohears.tacview;

import com.onewhohears.tacview.client.event.TVClientEventHandlers;
import com.onewhohears.tacview.common.core.EntityRecorders;
import com.onewhohears.tacview.common.event.TVCommonEventHandlers;
import com.onewhohears.tacview.init.TVModEntities;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

public final class TacViewMod {
    public static final String MOD_ID = "tacview";

    public static boolean isDHLoaded = false;

    public static void init() {
        TVCommonEventHandlers.init();
        EntityRecorders.registerDefaultRecorders();
        TVModEntities.register();
        if (Platform.getEnvironment() == Env.CLIENT) {
            TVClientEventHandlers.init();
        }
        isDHLoaded = Platform.isModLoaded("distanthorizons");
    }
}
