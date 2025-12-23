package com.onewhohears.tacview.core;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MoreRecorders {
    public static abstract class AbstractLivingRec<K extends EntityKeyframe<E>, E extends LivingEntity> extends EntityRecorder<K, E> {
        public final KeyframeValue.FloatV<E> health = registerFloatValue("health", LivingEntity::getHealth);
        public AbstractLivingRec(@NotNull E entity, int recordRate) {
            super(entity, recordRate);
        }
        public AbstractLivingRec(@NotNull JsonObject data) {
            super(data);
        }
        @Override
        protected boolean shouldRecord(@NotNull LivingEntity entity) {
            return entity.getHealth() > 0;
        }
    }
    public static class LivingRec extends AbstractLivingRec<EntityKeyframe<LivingEntity>, LivingEntity> {
        public LivingRec(@NotNull LivingEntity entity, int recordRate) {
            super(entity, recordRate);
        }
        public LivingRec(@NotNull JsonObject data) {
            super(data);
        }
        @Override
        protected @Nullable EntityKeyframe<LivingEntity> readKeyframe(@NotNull JsonObject keyframe) {
            return new EntityKeyframe<>(keyframe);
        }
        @Override
        protected EntityKeyframe<LivingEntity> newKeyFrame(@NotNull LivingEntity entity) {
            return new EntityKeyframe<>(entity);
        }
    }
    public static class PlayerRec extends AbstractLivingRec<EntityKeyframe<Player>, Player> {
        public PlayerRec(@NotNull Player entity, int recordRate) {
            super(entity, recordRate);
        }
        public PlayerRec(@NotNull JsonObject data) {
            super(data);
        }
        @Override
        protected @Nullable EntityKeyframe<Player> readKeyframe(@NotNull JsonObject keyframe) {
            return new EntityKeyframe<>(keyframe);
        }
        @Override
        protected EntityKeyframe<Player> newKeyFrame(@NotNull Player entity) {
            return new EntityKeyframe<>(entity);
        }
    }
}
