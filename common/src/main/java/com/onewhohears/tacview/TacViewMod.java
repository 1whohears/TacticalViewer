package com.onewhohears.tacview;

import com.onewhohears.tacview.core.EntityRecorders;

public final class TacViewMod {
    public static final String MOD_ID = "tacview";

    public static void init() {
        EntityRecorders.registerDefaultRecorders();
    }
}
