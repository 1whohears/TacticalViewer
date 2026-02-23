package com.onewhohears.tacview.client.core;

import com.onewhohears.tacview.common.entity.TacViewEntity;
import org.jetbrains.annotations.NotNull;

public class ClientPlayback {

    private final TacViewEntity parent;

    public ClientPlayback(@NotNull TacViewEntity parent) {
        this.parent = parent;
    }

    public void render()

}
