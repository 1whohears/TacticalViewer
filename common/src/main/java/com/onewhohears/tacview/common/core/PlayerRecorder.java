package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onewhohears.onewholibs.util.UtilMCText;
import com.onewhohears.tacview.client.core.ClientPlayback;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerRecorder extends MoreRecorders.AbstractLivingRec<EntityKeyframe<Player>, Player> {

    public PlayerRecorder(@NotNull Player entity, int recordRate) {
        super(entity, recordRate, EntityGetter::getPlayerByUUID);
    }

    public PlayerRecorder(@NotNull JsonObject data, @NotNull RecordingSession session) {
        super(data, EntityGetter::getPlayerByUUID, session);
    }

    @Override
    public void onPlaybackRender(@NotNull Player entity, @NotNull ClientPlayback playback,
                                 PoseStack stack, float yaw, @NotNull Vec3 renderPos, float partialTick,
                                 MultiBufferSource buffer, int packedLight) {
        super.onPlaybackRender(entity, playback, stack, yaw, renderPos, partialTick, buffer, packedLight);
        // TODO render triangle over player heads to make it easier to find them
    }

    @Override
    protected void extraRecordLogic(@NotNull RecordingSession session, @NotNull Player player) {
        if (player.isPassenger()) {
            session.addEntityToRecord(player.getRootVehicle());
        }
    }

    @Override
    protected @Nullable EntityKeyframe<Player> readKeyframe(@NotNull JsonObject keyframe) {
        return new MoreEntityKeyframes.PlayerKeyframe(keyframe);
    }

    @Override
    protected EntityKeyframe<Player> newKeyframe(@NotNull Player entity) {
        return new MoreEntityKeyframes.PlayerKeyframe(entity);
    }

    @Override
    protected EntityKeyframe<Player> emptyKeyframe() {
        return new MoreEntityKeyframes.PlayerKeyframe();
    }

    @Override
    public void onRecordingStart(@NotNull RecordingSession session) {
        super.onRecordingStart(session);
        Player player = getEntity();
        if (player == null) return;
        player.sendSystemMessage(UtilMCText.literal("TACVIEW: You will be recorded in session "
                +session.getSessionId()+" for the next "+session.getMaxLength()+" ticks!"));
    }

    @Override
    public void onRecordingFinish(@NotNull RecordingSession session) {
        super.onRecordingFinish(session);
        Player player = getEntity();
        if (player == null) return;
        player.sendSystemMessage(UtilMCText.literal("TACVIEW: Session "+session.getSessionId()
                +" has finished recording!"));
    }
}
