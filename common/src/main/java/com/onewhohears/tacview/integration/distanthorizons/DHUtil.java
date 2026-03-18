package com.onewhohears.tacview.integration.distanthorizons;

import com.onewhohears.tacview.client.core.HeightMapData;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class DHUtil {

    public static HeightMapData getHeightMap(ClientLevel level, Vec3 minBound, Vec3 maxBound, int lod) {
        Iterable<IDhApiLevelWrapper> levelWrappers = DhApi.Delayed.worldProxy.getAllLoadedLevelsWithDimensionNameLike(
                level.dimension().location().getPath());

        Vec3 size = maxBound.subtract(minBound);
        int xLength = (int)Math.ceil(size.x / lod);
        int zLength = (int)Math.ceil(size.z / lod);
        int[][] heightMap = new int[xLength][zLength];
        int[][] colorMap = new int[xLength][zLength];

        if (!levelWrappers.iterator().hasNext()) return new HeightMapData(xLength, zLength);
        IDhApiLevelWrapper levelWrapper = levelWrappers.iterator().next();

        int yPos = (int) maxBound.y;
        for (int x = 0; x < heightMap.length; ++x) {
            for (int z = 0; z < heightMap[x].length; ++z) {
                int xPos = (int) (minBound.x + x * lod);
                int zPos = (int) (minBound.z + z * lod);
                DhApiResult<DhApiTerrainDataPoint> point = DhApi.Delayed.terrainRepo.getSingleDataPointAtBlockPos(
                        levelWrapper, xPos, yPos, zPos, getTerrainCache());
                if (!point.success || point.payload == null) continue;
                int h;
                if (point.payload.topYBlockPos > 200) {
                    h = point.payload.bottomYBlockPos;
                } else {
                    h = point.payload.topYBlockPos;
                }
                heightMap[x][z] = h;
                h -= 1;
                point = DhApi.Delayed.terrainRepo.getSingleDataPointAtBlockPos(
                        levelWrapper, xPos, h, zPos, getTerrainCache());
                int color = getColor(point.payload.blockStateWrapper, level, xPos, h, yPos);
                colorMap[x][z] = color;
            }
        }
        return new HeightMapData(heightMap, colorMap);
    }

    private static IDhApiTerrainDataCache TERRAIN_CACHE = null;

    private static IDhApiTerrainDataCache getTerrainCache() {
        if (TERRAIN_CACHE == null) {
            TERRAIN_CACHE = DhApi.Delayed.terrainRepo.createSoftCache();
        }
        return TERRAIN_CACHE;
    }

    public static int getColor(IDhApiBlockStateWrapper state, Level level, int x, int y, int z) {
        if (!(state.getWrappedMcObject() instanceof BlockState blockState)) return 0;
        BlockPos blockPos = new BlockPos(x, y, z);
        return blockState.getMapColor(level, blockPos).calculateRGBColor(MapColor.Brightness.NORMAL);
    }

}
