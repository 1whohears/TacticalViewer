package com.onewhohears.tacview.common.core;

public final class RecordEventManager {
    // TODO some kind of record event system to capture player attacks/shielding/missile launches



    public static RecordEventManager get() {
        return SessionManager.get().getEventManager();
    }
}
