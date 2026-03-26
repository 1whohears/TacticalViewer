package com.onewhohears.tacview;

import com.onewhohears.tacview.client.core.ClientPlayback;
import com.onewhohears.tacview.client.core.HeightMapData;
import com.onewhohears.tacview.integration.distanthorizons.DHUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class TVDependencySafety {

    public static HeightMapData getDHHeightMap(ClientLevel level, Vec3 minBound, Vec3 maxBound, int lod, boolean refresh) {
        if (TacViewMod.isDHLoaded) return DHUtil.getHeightMap(level, minBound, maxBound, lod, refresh);
        Vec3 size = maxBound.subtract(minBound);
        int xLength = (int)Math.ceil(size.x / lod);
        int zLength = (int)Math.ceil(size.z / lod);
        return new HeightMapData(xLength, zLength);
    }

    public static void onClientPlaybackTick(@NotNull ClientPlayback playback) {
        if (TacViewMod.isDHLoaded) DHUtil.onClientPlaybackTick(playback);
    }

    public static void clearDHCache() {
        if (TacViewMod.isDHLoaded) DHUtil.clearTerrainCache();
    }

}
