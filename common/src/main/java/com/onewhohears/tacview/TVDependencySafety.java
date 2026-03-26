package com.onewhohears.tacview;

import com.onewhohears.tacview.client.core.ClientPlayback;
import com.onewhohears.tacview.integration.distanthorizons.DHUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class TVDependencySafety {

    public static void onClientPlaybackTick(@NotNull ClientPlayback playback, ClientLevel level,
                                            Vec3 minBound, Vec3 maxBound, int lod) {
        if (TacViewMod.isDHLoaded) DHUtil.onClientPlaybackTick(playback, level, minBound, maxBound, lod);
    }

    public static void clearDHCache() {
        if (TacViewMod.isDHLoaded) DHUtil.clearTerrainCache();
    }

}
