package com.onewhohears.tacview;

import com.onewhohears.tacview.client.core.HeightMapData;
import com.onewhohears.tacview.integration.distanthorizons.DHUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public class TVDependencySafety {

    public static HeightMapData getDHHeightMap(ClientLevel level, Vec3 minBound, Vec3 maxBound, int lod) {
        if (TacViewMod.isDHLoaded) return DHUtil.getHeightMap(level, minBound, maxBound, lod);
        Vec3 size = maxBound.subtract(minBound);
        int xLength = (int)Math.ceil(size.x / lod);
        int zLength = (int)Math.ceil(size.z / lod);
        return new HeightMapData(xLength, zLength);
    }

}
