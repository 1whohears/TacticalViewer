package com.onewhohears.tacview.integration.distanthorizons;

import com.mojang.logging.LogUtils;
import com.onewhohears.tacview.Config;
import com.onewhohears.tacview.client.core.ClientPlayback;
import com.onewhohears.tacview.client.core.HeightMapData;
import com.onewhohears.tacview.client.core.TVClientManager;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class DHUtil {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void onClientPlaybackTick(@NotNull ClientPlayback playback, ClientLevel level,
                                            Vec3 minBound, Vec3 maxBound, int lod) {
        HeightMapData heightMap = playback.getHeightMap();
        if (heightMap == null) return;
        long startTime = System.currentTimeMillis();

        int totalTiles = heightMap.heights().length * heightMap.heights()[0].length;
        if (playback.calculatedHeights >= totalTiles) return;

        Iterable<IDhApiLevelWrapper> levelWrappers = DhApi.Delayed.worldProxy.getAllLoadedLevelsWithDimensionNameLike(
                level.dimension().location().getPath());
        if (!levelWrappers.iterator().hasNext()) {
            LOGGER.warn("DHUtil returned an empty height map because no level wrapper was found!");
            return;
        }
        IDhApiLevelWrapper levelWrapper = levelWrappers.iterator().next();

        int cols = heightMap.heights()[0].length;
        int firstX = playback.calculatedHeights / cols;
        int firstZ = playback.calculatedHeights % cols;
        boolean oneGoodPayload = false;
        int yPos = Math.min((int) maxBound.y, 200);

        for (int x = firstX; x < heightMap.heights().length; ++x) {
            for (int z = firstZ; z < heightMap.heights()[x].length; ++z) {
                if (TVClientManager.get().HM_TILE_GEN_TICK_COUNT >= Config.CLIENT.heightMapMaxGenTilesPerTick.get()) {
                    return;
                }
                long timeDiff = System.currentTimeMillis() - startTime;
                if (timeDiff > 50) {
                    LOGGER.warn("DHUtil onClientPlaybackTick taking {} millis!" +
                            " Go to config and reduce heightMapMaxGenTilesPerTick", timeDiff);
                    return;
                }
                int xPos = (int) (minBound.x + x * lod);
                int zPos = (int) (minBound.z + z * lod);
                DhApiResult<DhApiTerrainDataPoint> point = DhApi.Delayed.terrainRepo.getSingleDataPointAtBlockPos(
                        levelWrapper, xPos, yPos, zPos, getTerrainCache());
                if (!point.success || point.payload == null) continue;
                oneGoodPayload = true;
                // sometimes topYBlockPos is the build height limit, sometimes bottomYBlockPos is the bottom build limit.
                // I still don't know why this happens or what the correct way to determine which height to use.
                // this is a hacky way to figure out what the actual world height is.
                int h;
                if (point.payload.topYBlockPos > 200) {
                    h = point.payload.bottomYBlockPos;
                } else {
                    h = point.payload.topYBlockPos;
                }
                heightMap.heights()[x][z] = h;
                h -= 1;
                point = DhApi.Delayed.terrainRepo.getSingleDataPointAtBlockPos(
                        levelWrapper, xPos, h, zPos, getTerrainCache());
                int color = getColor(point.payload.blockStateWrapper, level, xPos, h, yPos);
                heightMap.colors()[x][z] = color;

                TVClientManager.get().HM_TILE_GEN_TICK_COUNT++;
                playback.calculatedHeights++;
                TVClientManager.get().PREV_UPDATE_TIME = System.currentTimeMillis();
                TVClientManager.get().CLEARED_DH_CACHE = false;
            }
            firstZ = 0;
        }

        if (!oneGoodPayload) {
            LOGGER.warn("DHUtil playback tick returned an empty height map because" +
                    " no DHApi data point calls were successful!" +
                    " LOD = {}", lod);
        }
    }

    private static IDhApiTerrainDataCache TERRAIN_CACHE = null;

    private static IDhApiTerrainDataCache getTerrainCache() {
        if (TERRAIN_CACHE == null) {
            TERRAIN_CACHE = DhApi.Delayed.terrainRepo.createSoftCache();
        }
        return TERRAIN_CACHE;
    }

    public static void clearTerrainCache() {
        getTerrainCache().clear();
    }

    public static int getColor(IDhApiBlockStateWrapper state, Level level, int x, int y, int z) {
        if (!(state.getWrappedMcObject() instanceof BlockState blockState)) return 0;
        BlockPos blockPos = new BlockPos(x, y, z);
        return blockState.getMapColor(level, blockPos).calculateRGBColor(MapColor.Brightness.NORMAL);
    }

}
