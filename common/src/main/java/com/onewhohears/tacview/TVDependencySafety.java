package com.onewhohears.tacview;

import com.onewhohears.tacview.integration.distanthorizons.DHUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public class TVDependencySafety {

    public static int[][] getDHHeightMap(ClientLevel level, Vec3 minBound, Vec3 maxBound) {
        if (TacViewMod.isDHLoaded) return DHUtil.getHeightMap(level, minBound, maxBound);
        Vec3 size = maxBound.subtract(minBound);
        return new int[(int)Math.ceil(size.x)][(int)Math.ceil(size.z)];
    }

}
