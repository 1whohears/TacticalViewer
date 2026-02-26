package com.onewhohears.tacview.client.core;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.tacview.common.core.EntityKeyframe;
import com.onewhohears.tacview.common.core.EntityRecorder;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class ClientPlayback {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final TacViewEntity parent;
    private final Map<UUID,Entity> fakeEntities = new HashMap<>();

    private final Set<String> bannedEntityTypes = new HashSet<>();

    public ClientPlayback(@NotNull TacViewEntity parent) {
        this.parent = parent;
    }

    /**
     * CLIENT SIDE ONLY
     */
    public void render(float yaw, float partialTick, PoseStack stack, MultiBufferSource buffer, int packedLight) {
        String sessionId = parent.getSessionId();
        RecordingSession session = SessionManager.get().getSession(sessionId);
        if (session == null) {
            TVClientManager.get().requestRecordingSessionFromServer(sessionId);
            return;
        }

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
        Vec3 center = minBound.add(size.scale(0.5));

        Minecraft m = Minecraft.getInstance();
        Vec3 camPos = m.gameRenderer.getMainCamera().getPosition();

        float scale = (float) Math.min(height/size.y, Math.min(width/size.x, width/size.z));

        stack.pushPose();

        session.forEachRecorder((uuid, recorder) -> {
            stack.pushPose();

            String entityTypeStr = recorder.entityType.get();
            if (bannedEntityTypes.contains(entityTypeStr)) return;

            Entity fake = getCreateFakeEntity(uuid, entityTypeStr, recorder);
            if (fake == null) return;

            try {
                EntityKeyframe keyframe = recorder.interpolate(tick, pt);

                keyframe.writeToFakeEntity(fake);

                // TODO optional additional render logic

                float f = fake.getYRot();
                double dx = Mth.lerp(partialTick, fake.xOld, fake.getX());
                double dy = Mth.lerp(partialTick, fake.yOld, fake.getY());
                double dz = Mth.lerp(partialTick, fake.zOld, fake.getZ());

                Vec3 d = new Vec3(dx, dy, dz).subtract(center).scale(scale).subtract(camPos);

                stack.scale(scale, scale, scale);

                m.getEntityRenderDispatcher().render(
                        fake, d.x, d.y, d.z, f, partialTick, stack, buffer, packedLight
                );
            } catch (ReportedException e) {
                banEntityType(entityTypeStr, e.getReport().getFriendlyReport());
            }
            stack.popPose();
        });
        stack.popPose();
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
        // TODO optional addition entity setup
        fakeEntities.put(uuid, e);
        return e;
    }

    private void banEntityType(String entityTypeStr, String reason) {
        bannedEntityTypes.add(entityTypeStr);
        LOGGER.error("Tac View Attempted to render a fake entity and an error was thrown. " +
                "Will not try to render this entity type until the game is reloaded. " +
                "The error that would have crashed the game is the following:");
        LOGGER.error(reason);
    }
}
