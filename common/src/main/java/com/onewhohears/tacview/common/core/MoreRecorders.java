package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;

public class MoreRecorders {
    public static class DefaultEntityRecorder extends EntityRecorder<EntityKeyframe<Entity>,Entity> {
        public DefaultEntityRecorder(@NotNull JsonObject data) {
            super(data, (level, uuid) -> null);
        }
        @Override
        protected @Nullable EntityKeyframe<Entity> readKeyframe(@NotNull JsonObject keyframe) {
            return new EntityKeyframe<>(keyframe);
        }
        @Override
        protected EntityKeyframe<Entity> newKeyframe(@NotNull Entity entity) {
            return new EntityKeyframe<>(entity);
        }
        @Override
        protected EntityKeyframe<Entity> emptyKeyframe() {
            return new EntityKeyframe<>();
        }
        public DefaultEntityRecorder(@NotNull Entity entity, int recordRate) {
            super(entity, recordRate, (level, uuid) -> null);
        }
    }
    public static abstract class AbstractLivingRec<K extends EntityKeyframe<E>, E extends LivingEntity> extends EntityRecorder<K, E> {
        public AbstractLivingRec(@NotNull E entity, int recordRate, @NotNull BiFunction<ServerLevel,UUID,E> entityFinder) {
            super(entity, recordRate, entityFinder);
        }
        public AbstractLivingRec(@NotNull JsonObject data, @NotNull BiFunction<ServerLevel,UUID,E> entityFinder) {
            super(data, entityFinder);
        }
        @Override
        protected boolean shouldRecord(@NotNull LivingEntity entity) {
            return !entity.isRemoved() && entity.getHealth() > 0;
        }
    }
    public static class LivingRec extends AbstractLivingRec<EntityKeyframe<LivingEntity>, LivingEntity> {
        public LivingRec(@NotNull LivingEntity entity, int recordRate) {
            super(entity, recordRate, (level, uuid) -> null);
        }
        public LivingRec(@NotNull JsonObject data) {
            super(data, (level, uuid) -> null);
        }
        @Override
        protected @Nullable EntityKeyframe<LivingEntity> readKeyframe(@NotNull JsonObject keyframe) {
            return new MoreEntityKeyframes.LivingEntityKeyframe(keyframe);
        }
        @Override
        protected EntityKeyframe<LivingEntity> newKeyframe(@NotNull LivingEntity entity) {
            return new MoreEntityKeyframes.LivingEntityKeyframe(entity);
        }
        @Override
        protected EntityKeyframe<LivingEntity> emptyKeyframe() {
            return new MoreEntityKeyframes.LivingEntityKeyframe();
        }
    }
}
