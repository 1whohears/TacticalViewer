package com.onewhohears.tacview.client.core;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.onewholibs.util.math.UtilGeometry;
import com.onewhohears.tacview.TVDependencySafety;
import com.onewhohears.tacview.TacViewMod;
import com.onewhohears.tacview.common.core.EntityKeyframe;
import com.onewhohears.tacview.common.core.EntityRecorder;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.core.recordevent.RecordEvent;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.util.*;

public class ClientPlayback {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int RED = 0x22, GREEN = 0x99, BLUE = 0x22;
    private static final long HEIGHT_MAP_UPDATE_RATE = 4000;
    private static final double MAX_TILES_INV = 1d / 10000d;

    private final TacViewEntity parent;
    private final Map<UUID,Entity> fakeEntities = new HashMap<>();

    private final List<Component> overlayEntityInfo = new ArrayList<>();
    private final Set<String> bannedEntityTypes = new HashSet<>();

    private int[][] heightMap = null;
    private long heightMapUpdateTime = 0;

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
        long tick  = parent.getPlaybackTick();
        float width = parent.getWidth();
        float height = parent.getHeight();
        Vec3 minBound = session.getMinBound();
        Vec3 maxBound = session.getMaxBound();
        Vec3 size = maxBound.subtract(minBound);
        float scale = (float) Math.min(height/size.y, Math.min(width/size.x, width/size.z));
        Vec3 center = minBound.add(size.multiply(0.5, 0, 0.5));

        overlayEntityInfo.clear();
        Minecraft m = Minecraft.getInstance();
        session.forEachRecorder((uuid, recorder) -> {
            String entityTypeStr = recorder.entityType.get();
            if (bannedEntityTypes.contains(entityTypeStr)) return;

            Entity fake = getCreateFakeEntity(uuid, entityTypeStr, recorder);
            if (fake == null) return;

            recorder.onPlaybackTick(fake, this);

            EntityKeyframe keyframe = recorder.interpolate(tick, 0);
            if (!keyframe.vehicleUUID.get().isEmpty()) {
                UUID vehicleUUID = UUID.fromString(keyframe.vehicleUUID.get());
                EntityRecorder vehicleRecorder = session.getRecorder(vehicleUUID);
                if (vehicleRecorder == null) return;
                Entity vehicle = fakeEntities.get(vehicleUUID);
                if (vehicle == null) return;
                vehicleRecorder.onPlaybackVehicleTick(vehicle, fake, this);
            } else if (fake.isPassenger()) {
                fake.stopRiding();
            }

            Vec3 eye = m.player.getEyePosition();
            Vec3 d = fake.position().subtract(center);
            Vec3 worldPos = parent.position().add(d.add(0,fake.getBbHeight()*0.5,0).scale(scale));
            boolean inCone = UtilGeometry.isPointInsideCone(worldPos, eye, m.player.getLookAngle(),
                    Math.abs(Math.atan2(fake.getBbWidth()*scale, worldPos.distanceTo(eye)))
                            *Mth.RAD_TO_DEG*4, width);
            if (inCone) recorder.addOverlayInfo(overlayEntityInfo, fake, keyframe);
        });
        List<RecordEvent> eventsAtTick = session.getRecordEventsAtTick(tick);
        eventsAtTick.forEach(event -> event.onEventPlayback(this));
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
            int lod = (int) Math.ceil(Math.sqrt(size.x * size.z * MAX_TILES_INV));
            stack.translate(0, 0.01f / scale, 0);
            m.getProfiler().push("Tac View Replay Gen Height Map");
            updateHeightMap(m.level, minBound, maxBound, lod);
            m.getProfiler().pop();
            m.getProfiler().push("Tac View Replay Render Terrain");
            VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
            int minX = (int) (minBound.x - center.x), minZ = (int) (minBound.z - center.z);
            float minY = m.level.getMinBuildHeight(), maxY = m.level.getMaxBuildHeight();
            for (int x = 0; x < heightMap.length; ++x) {
                for (int z = 0; z < heightMap[x].length; ++z) {
                    int h = (int) (heightMap[x][z] - center.y);
                    int green = (int) Math.min((heightMap[x][z] - minY) / (maxY - minY) * 0xDD + 0x22, 0xFF);
                    stack.pushPose();
                    stack.translate(minX + x * lod, h, minZ + z * lod);
                    drawTopSquare(stack, consumer, packedLight, lod, RED, green, BLUE);

                    if (x < heightMap.length - 1 && heightMap[x + 1][z] != heightMap[x][z]) {
                        stack.pushPose();
                        stack.translate(lod, 0, 0);
                        drawXSquare(stack, consumer, packedLight, lod, RED, green, BLUE,
                                heightMap[x + 1][z] - heightMap[x][z]);
                        stack.popPose();
                    }
                    if (z < heightMap[x].length - 1 && heightMap[x][z + 1] != heightMap[x][z]) {
                        stack.pushPose();
                        stack.translate(0, 0, lod);
                        drawZSquare(stack, consumer, packedLight, lod, RED, green, BLUE,
                                heightMap[x][z + 1] - heightMap[x][z]);
                        stack.popPose();
                    }

                    stack.popPose();
                }
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

                if (tick <= keyframe.getTick() + recorder.recordRate && tick >= keyframe.getTick() - recorder.recordRate) {
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

    private void updateHeightMap(ClientLevel level, Vec3 minBound, Vec3 maxBound, int lod) {
        if (heightMap == null || System.currentTimeMillis() - heightMapUpdateTime >= HEIGHT_MAP_UPDATE_RATE) {
            heightMap = TVDependencySafety.getDHHeightMap(level, minBound, maxBound, lod);
            heightMapUpdateTime = System.currentTimeMillis();
        }
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

    private void drawXSquare(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, float size, int red, int green, int blue, int height) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        consumer.vertex(matrix, 0, 0, 0)
                .color(red, green, blue, 255)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, 0, 0, size)
                .color(red, green, blue, 255)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, 0, height, size)
                .color(red, green, blue, 255)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, 0, height, 0)
                .color(red, green, blue, 255)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
    }

    private void drawZSquare(PoseStack poseStack, VertexConsumer consumer,
                             int packedLight, float size, int red, int green, int blue, int height) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        consumer.vertex(matrix, 0, 0, 0)
                .color(red, green, blue, 255)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, size, 0, 0)
                .color(red, green, blue, 255)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, size, height, 0)
                .color(red, green, blue, 255)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, 0, height, 0)
                .color(red, green, blue, 255)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
    }

    private void drawTopSquare(PoseStack poseStack, VertexConsumer consumer,
                               int packedLight, float size, int red, int green, int blue) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        consumer.vertex(matrix, 0, 0, 0)
                .color(red, green, blue, 255)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, 0, 0, size)
                .color(red, green, blue, 255)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, size, 0, size)
                .color(red, green, blue, 255)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
        consumer.vertex(matrix, size, 0, 0)
                .color(red, green, blue, 255)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
    }

    public TacViewEntity getParent() {
        return parent;
    }

    public List<Component> getOverlayEntityInfo() {
        return overlayEntityInfo;
    }
}
