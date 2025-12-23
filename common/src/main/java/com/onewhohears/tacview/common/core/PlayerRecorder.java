package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerRecorder extends MoreRecorders.AbstractLivingRec<EntityKeyframe<Player>, Player> {

    public PlayerRecorder(@NotNull Player entity, int recordRate) {
        super(entity, recordRate);
    }

    public PlayerRecorder(@NotNull JsonObject data) {
        super(data);
    }

    @Override
    protected void extraRecordLogic(@NotNull RecordingSession session, @NotNull Player player) {

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
}
