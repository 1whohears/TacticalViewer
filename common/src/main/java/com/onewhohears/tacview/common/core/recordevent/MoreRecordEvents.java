package com.onewhohears.tacview.common.core.recordevent;

import com.google.gson.JsonObject;
import com.onewhohears.onewholibs.util.UtilParse;
import com.onewhohears.tacview.client.core.ClientPlayback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.onewhohears.tacview.common.core.KeyframeValue.UUIDV.DEFAULT_UUID_STR;

public class MoreRecordEvents {
    public static class PlayerAttack extends EntityRecordEvent<Player> {
        private UUID targetUUID;
        public PlayerAttack(@NotNull Player player, @NotNull Entity target) {
            super("player_attack", player);
            targetUUID = target.getUUID();
        }
        public PlayerAttack(@NotNull JsonObject data) {
            super(data);
        }
        @Override
        protected void addSaveData(@NotNull JsonObject data) {
            data.addProperty("targetUUID", targetUUID.toString());
        }
        @Override
        protected void readSaveData(@NotNull JsonObject data) {
            String targetUUIDStr = UtilParse.getStringSafe(data, "targetUUID", DEFAULT_UUID_STR);
            targetUUID = UUID.fromString(targetUUIDStr);
        }
        @Override
        protected void onEventPlayback(@NotNull ClientPlayback playback, @NotNull Player entity) {
            entity.attackAnim = 0.001f;
        }
    }
    public static class LivingDeath extends EntityRecordEvent<LivingEntity> {
        public LivingDeath(@NotNull LivingEntity entity) {
            super("living_death", entity);
        }
        public LivingDeath(@NotNull JsonObject data) {
            super(data);
        }
        @Override protected void addSaveData(@NotNull JsonObject data) {}
        @Override protected void readSaveData(@NotNull JsonObject data) {}
        @Override
        protected void onEventPlayback(@NotNull ClientPlayback playback, @NotNull LivingEntity entity) {

        }
    }
}
