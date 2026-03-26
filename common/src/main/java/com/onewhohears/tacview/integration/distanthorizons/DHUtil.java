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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class DHUtil {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private static HeightMapData HEIGHT_MAP = null;
    private static ClientLevel LEVEL = null;
    private static Vec3 MIN_BOUND = null, MAX_BOUND = null;
    private static int LOD;

    public static void onClientPlaybackTick(@NotNull ClientPlayback playback) {
        if (HEIGHT_MAP == null) return;
        long startTime = System.currentTimeMillis();

        int totalTiles = HEIGHT_MAP.heights().length * HEIGHT_MAP.heights()[0].length;
        if (playback.calculatedHeights >= totalTiles) return;

        Iterable<IDhApiLevelWrapper> levelWrappers = DhApi.Delayed.worldProxy.getAllLoadedLevelsWithDimensionNameLike(
                LEVEL.dimension().location().getPath());
        if (!levelWrappers.iterator().hasNext()) {
            LOGGER.warn("DHUtil returned an empty height map because no level wrapper was found!");
            return;
        }
        IDhApiLevelWrapper levelWrapper = levelWrappers.iterator().next();

        int cols = HEIGHT_MAP.heights()[0].length;
        int firstX = playback.calculatedHeights / cols;
        int firstZ = playback.calculatedHeights % cols;
        boolean oneGoodPayload = false;
        int yPos = Math.min((int) MAX_BOUND.y, 200);

        for (int x = firstX; x < HEIGHT_MAP.heights().length; ++x) {
            for (int z = firstZ; z < HEIGHT_MAP.heights()[x].length; ++z) {
                if (TVClientManager.get().HM_TILE_GEN_TICK_COUNT >= Config.CLIENT.heightMapMaxGenTilesPerTick.get()) {
                    return;
                }
                long timeDiff = System.currentTimeMillis() - startTime;
                if (timeDiff > 50) {
                    LOGGER.warn("DHUtil onClientPlaybackTick taking {} millis!" +
                            " Go to config and reduce heightMapMaxGenTilesPerTick", timeDiff);
                    return;
                }
                int xPos = (int) (MIN_BOUND.x + x * LOD);
                int zPos = (int) (MIN_BOUND.z + z * LOD);
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
                HEIGHT_MAP.heights()[x][z] = h;
                h -= 1;
                point = DhApi.Delayed.terrainRepo.getSingleDataPointAtBlockPos(
                        levelWrapper, xPos, h, zPos, getTerrainCache());
                int color = getColor(point.payload.blockStateWrapper, LEVEL, xPos, h, yPos);
                HEIGHT_MAP.colors()[x][z] = color;

                TVClientManager.get().HM_TILE_GEN_TICK_COUNT++;
                playback.calculatedHeights++;
            }
            firstZ = 0;
        }

        if (playback.calculatedHeights >= totalTiles) {
            clearTerrainCache();
        }

        if (!oneGoodPayload) {
            LOGGER.warn("DHUtil playback tick returned an empty height map because" +
                    " no DHApi data point calls were successful!" +
                    " LOD = {}", LOD);
        }
    }

    public static HeightMapData getHeightMap(ClientLevel level, Vec3 minBound, Vec3 maxBound, int lod, boolean refresh) {
        LEVEL = level;
        MIN_BOUND = minBound;
        MAX_BOUND = maxBound;
        LOD = lod;

        if (refresh || HEIGHT_MAP == null) {
            Vec3 size = maxBound.subtract(minBound);
            int xLength = (int)Math.ceil(size.x / lod);
            int zLength = (int)Math.ceil(size.z / lod);
            int[][] heightMap = new int[xLength][zLength];
            int[][] colorMap = new int[xLength][zLength];
            HEIGHT_MAP = new HeightMapData(heightMap, colorMap);
        }

        return HEIGHT_MAP;
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
