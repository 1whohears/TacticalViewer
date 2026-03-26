package com.onewhohears.tacview.client.core;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.math.UtilGeometry;
import com.onewhohears.tacview.Config;
import com.onewhohears.tacview.TVDependencySafety;
import com.onewhohears.tacview.TacViewMod;
import com.onewhohears.tacview.common.core.*;
import com.onewhohears.tacview.common.core.recordevent.RecordEvent;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ClientPlayback {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int RED = 0x22, GREEN = 0x99, BLUE = 0x22;
    private static final long HEIGHT_MAP_UPDATE_RATE = 4000;

    private final TacViewEntity parent;
    private final Map<UUID,Entity> fakeEntities = new HashMap<>();

    private final List<Component> overlayEntityInfo = new ArrayList<>();
    private final Set<String> bannedEntityTypes = new HashSet<>();

    public int calculatedHeights = 0;
    private int meshedHeights = 0;

    private HeightMapData heightMap = null;
    private long heightMapUpdateTime = 0;
    private long heightMapUpdateMeshTime = 0;
    private VertexBuffer heightmapBuffer;
    private boolean heightmapMeshDirty = true;
    private Vec3 center = Vec3.ZERO;
    private float scale = 1;
    private Entity lookAtFakeEntity = null;
    private String prevSessionId = "";

    public ClientPlayback(@NotNull TacViewEntity parent) {
        this.parent = parent;
    }

    /**
     * CLIENT SIDE ONLY
     */
    public void tick() {
        String sessionId = parent.getSessionId();
        RecordingSession session = SessionManager.get().getSession(sessionId);
        if (session == null || !session.isRecordingComplete()) {
            TVClientManager.get().requestRecordingSessionFromServer(sessionId);
            return;
        }
        if (!prevSessionId.equals(sessionId)) heightMap = null;

        long tick  = parent.getPlaybackTick();
        float width = parent.getWidth();
        float height = parent.getHeight();
        Vec3 minBound = session.getMinBound();
        Vec3 maxBound = session.getMaxBound();
        Vec3 size = maxBound.subtract(minBound);
        center = minBound.add(size.multiply(0.5, 0, 0.5));
        scale = (float) Math.min(height/size.y, Math.min(width/size.x, width/size.z));

        overlayEntityInfo.clear();
        lookAtFakeEntity = null;
        Minecraft m = Minecraft.getInstance();
        session.forEachRecorder((uuid, recorder) -> {
            String entityTypeStr = recorder.entityType.get();
            if (bannedEntityTypes.contains(entityTypeStr)) return;

            Entity fake = getCreateFakeEntity(uuid, entityTypeStr, recorder);
            if (fake == null) return;

            recorder.onPlaybackTick(fake, this);

            EntityKeyframe keyframe = recorder.interpolate(tick, 0);
            if (!keyframe.vehicleUUID.get().equals(KeyframeValue.UUIDV.DEFAULT_UUID)) {
                UUID vehicleUUID = keyframe.vehicleUUID.get();
                EntityRecorder vehicleRecorder = session.getRecorder(vehicleUUID);
                if (vehicleRecorder == null) return;
                Entity vehicle = fakeEntities.get(vehicleUUID);
                if (vehicle == null) return;
                vehicleRecorder.onPlaybackVehicleTick(vehicle, fake, this);
            } else if (fake.isPassenger()) {
                fake.stopRiding();
            }

            Vec3 eye = m.player.getEyePosition();
            Vec3 worldPos = getFakeWorldPos(fake);
            boolean inCone = UtilGeometry.isPointInsideCone(worldPos, eye, m.player.getLookAngle(),
                    Math.abs(Math.atan2(fake.getBbWidth()*scale, worldPos.distanceTo(eye)))
                            *Mth.RAD_TO_DEG*4, width);
            if (inCone) {
                recorder.addOverlayInfo(overlayEntityInfo, fake, keyframe);
                if (lookAtFakeEntity == null
                        || fake.distanceToSqr(m.player) < lookAtFakeEntity.distanceToSqr(m.player)) {
                    lookAtFakeEntity = fake;
                }
            }
        });
        List<RecordEvent> eventsAtTick = session.getRecordEventsAtTick(tick);
        eventsAtTick.forEach(event -> event.onEventPlayback(this));

        int lod = (int) Math.ceil(Math.sqrt(size.x * size.z / Config.CLIENT.maxHeightMapTiles.get()));
        TVDependencySafety.onClientPlaybackTick(this, m.level, minBound, maxBound, lod);
        prevSessionId = sessionId;
    }

    public Vec3 getFakeWorldPos(@NotNull Entity fake) {
        Vec3 d = fake.position().add(0,fake.getBbHeight()*0.5,0).subtract(center);
        return parent.position().add(d.scale(scale));
    }

    /**
     * CLIENT SIDE ONLY
     */
    public void render(float yaw, float partialTick, PoseStack stack, MultiBufferSource buffer, int packedLight) {
        String sessionId = parent.getSessionId();
        RecordingSession session = SessionManager.get().getSession(sessionId);
        if (session == null || !session.isRecordingComplete()) {
            TVClientManager.get().requestRecordingSessionFromServer(sessionId);
            return;
        }

        Minecraft m = Minecraft.getInstance();
        m.getProfiler().push("Tac View Replay Render");

        float width = parent.getWidth();
        float height = parent.getHeight();
        long tick = parent.getPlaybackTick();
        boolean paused = parent.isPaused();
        float pt = paused ? 0 : partialTick;
        //int tickRate = parent.getTickRate();
        //long nextTick = paused ? tick : tick + tickRate;

        Vec3 minBound = session.getMinBound();
        Vec3 maxBound = session.getMaxBound();
        Vec3 size = maxBound.subtract(minBound);
        float scale = (float) Math.min(height/size.y, Math.min(width/size.x, width/size.z));
        Vec3 center = minBound.add(size.multiply(0.5, 0, 0.5));

        stack.pushPose();
        stack.scale(scale, scale, scale);

        if (TacViewMod.isDHLoaded) {
            int lod = (int) Math.ceil(Math.sqrt(size.x * size.z / Config.CLIENT.maxHeightMapTiles.get()));
            stack.translate(0, 0.01f / scale, 0);

            updateHeightMap(minBound, maxBound, lod);

            if (heightmapMeshDirty) {
                float minY = m.level.getMinBuildHeight(), maxY = m.level.getMaxBuildHeight();
                rebuildHeightmapMesh(center, lod, minY, maxY);
            }

            m.getProfiler().push("Tac View Replay Render Terrain");

            if (heightmapBuffer != null) {
                stack.pushPose();
                stack.translate(-size.x*0.5, 0, -size.z*0.5);

                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);

                heightmapBuffer.bind();
                heightmapBuffer.drawWithShader(stack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                VertexBuffer.unbind();

                stack.popPose();
            }

            m.getProfiler().pop();
        }

        m.getProfiler().push("Tac View Replay Render Entities");
        session.forEachRecorder((uuid, recorder) -> {
            stack.pushPose();

            String entityTypeStr = recorder.entityType.get();
            if (bannedEntityTypes.contains(entityTypeStr)) return;

            Entity fake = getCreateFakeEntity(uuid, entityTypeStr, recorder);
            if (fake == null) return;

            try {
                EntityKeyframe keyframe = recorder.interpolate(tick, pt);

                if (tick <= keyframe.getTick() + recorder.recordRate
                        && tick >= keyframe.getTick() - recorder.recordRate) {
                    // TODO don't render dead players/spectators
                    keyframe.writeToFakeEntity(fake);

                    float f = fake.getYRot();
                    Vec3 d = fake.position().subtract(center);

                    recorder.onPlaybackRender(fake, this, stack, f, d, partialTick, buffer, packedLight);

                    // TODO fix entities rendering under the height map

                    m.getEntityRenderDispatcher().render(fake, d.x, d.y, d.z, f, partialTick, stack, buffer, packedLight);
                }
            } catch (ReportedException e) {
                banEntityType(entityTypeStr, e.getReport().getFriendlyReport());
            }
            stack.popPose();
        });
        m.getProfiler().pop();

        stack.popPose();
        m.getProfiler().pop();
    }

    @Nullable
    public Entity getFakeEntityToTrack() {
        if (lookAtFakeEntity != null) return lookAtFakeEntity;
        Minecraft m = Minecraft.getInstance();
        Entity closestFake = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity entity : fakeEntities.values()) {
            double dist = m.player.distanceToSqr(entity);
            if (dist < closestDist) {
                closestFake = entity;
                closestDist = dist;
            }
        }
        return closestFake;
    }

    private void updateHeightMap(Vec3 minBound, Vec3 maxBound, int lod) {
        if (heightMap == null) {
            heightMap = createNewHeightMap(minBound, maxBound, lod);
            calculatedHeights = 0;
            meshedHeights = 0;
            heightMapUpdateTime = System.currentTimeMillis();
        }
        long timeDiff = System.currentTimeMillis() - heightMapUpdateTime;
        if (timeDiff >= Config.CLIENT.heightMapUpdateRate.get() * 1000) {
            calculatedHeights = 0;
            meshedHeights = 0;
            heightMapUpdateTime = System.currentTimeMillis();
        }
        long meshTimeDiff = System.currentTimeMillis() - heightMapUpdateMeshTime;
        if (heightMap != null && meshedHeights < calculatedHeights
                && meshTimeDiff > Config.CLIENT.heightMapMeshUpdateRate.get() * 1000) {
            heightmapMeshDirty = true;
            heightMapUpdateMeshTime = System.currentTimeMillis();
        }
    }

    private HeightMapData createNewHeightMap(Vec3 minBound, Vec3 maxBound, int lod) {
        Vec3 size = maxBound.subtract(minBound);
        int xLength = (int)Math.ceil(size.x / lod);
        int zLength = (int)Math.ceil(size.z / lod);
        int[][] heightMap = new int[xLength][zLength];
        int[][] colorMap = new int[xLength][zLength];
        return new HeightMapData(heightMap, colorMap);
    }

    @Nullable
    private Entity getCreateFakeEntity(UUID uuid, String entityTypeStr, EntityRecorder recorder) {
        if (fakeEntities.containsKey(uuid)) return fakeEntities.get(uuid);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        EntityType<?> type = UtilEntity.getEntityType(entityTypeStr, null);
        if (type == null) {
            banEntityType(entityTypeStr, "Entity Type does not exist");
            return null;
        }
        Entity e;
        if (type.toString().equals("entity.minecraft.player")) {
            GameProfile profile = new GameProfile(uuid, recorder.name.get());
            e = new RemotePlayer(level, profile);
        } else {
            e = type.create(level);
            if (e == null) {
                banEntityType(entityTypeStr, "Failed to create the entity");
                return null;
            }
        }
        recorder.onPlaybackEntitySetup(e, this);
        fakeEntities.put(uuid, e);
        return e;
    }

    @Nullable
    public Entity getEntity(UUID uuid) {
        return fakeEntities.get(uuid);
    }

    private void banEntityType(String entityTypeStr, String reason) {
        bannedEntityTypes.add(entityTypeStr);
        LOGGER.error("Tac View Attempted to render a fake entity and an error was thrown. " +
                "Will not try to render this entity type until the game is reloaded. " +
                "The error that would have crashed the game is the following:");
        LOGGER.error(reason);
    }

    public TacViewEntity getParent() {
        return parent;
    }

    public List<Component> getOverlayEntityInfo() {
        return overlayEntityInfo;
    }

    private void rebuildHeightmapMesh(Vec3 center, float lod, float minY, float maxY) {
        if (heightMap == null) return;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int x = 0; x < heightMap.heights().length; x++) {
            for (int z = 0; z < heightMap.heights()[x].length; z++) {

                int worldY = heightMap.heights()[x][z];
                float y = (float)(worldY - center.y);

                float baseX = x * lod;
                float baseZ = z * lod;

                int color = heightMap.colors()[x][z];
                Color c = new Color(color);
                color = FastColor.ARGB32.color(0xff, c.getBlue(), c.getGreen(), c.getRed());

                // TOP FACE
                buffer.vertex(baseX, y, baseZ).color(color).endVertex();
                buffer.vertex(baseX, y, baseZ + lod).color(color).endVertex();
                buffer.vertex(baseX + lod, y, baseZ + lod).color(color).endVertex();
                buffer.vertex(baseX + lod, y, baseZ).color(color).endVertex();

                // X WALL
                if (x < heightMap.heights().length - 1) {
                    int nextY = heightMap.heights()[x + 1][z];
                    if (nextY != worldY) {
                        float dy = nextY - worldY;

                        buffer.vertex(baseX + lod, y, baseZ).color(color).endVertex();
                        buffer.vertex(baseX + lod, y, baseZ + lod).color(color).endVertex();
                        buffer.vertex(baseX + lod, y + dy, baseZ + lod).color(color).endVertex();
                        buffer.vertex(baseX + lod, y + dy, baseZ).color(color).endVertex();
                    }
                }

                // Z WALL
                if (z < heightMap.heights()[x].length - 1) {
                    int nextY = heightMap.heights()[x][z + 1];
                    if (nextY != worldY) {
                        float dy = nextY - worldY;

                        buffer.vertex(baseX, y, baseZ + lod).color(color).endVertex();
                        buffer.vertex(baseX, y + dy, baseZ + lod).color(color).endVertex();
                        buffer.vertex(baseX + lod, y + dy, baseZ + lod).color(color).endVertex();
                        buffer.vertex(baseX + lod, y, baseZ + lod).color(color).endVertex();
                    }
                }
            }
        }

        if (heightmapBuffer != null) {
            heightmapBuffer.close();
        }

        heightmapBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        heightmapBuffer.bind();
        heightmapBuffer.upload(buffer.end());
        VertexBuffer.unbind();

        heightmapMeshDirty = false;
        meshedHeights = calculatedHeights;
    }

    @Nullable
    public HeightMapData getHeightMap() {
        return heightMap;
    }
}
