package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MoreRecorders {
    public static abstract class AbstractLivingRec<K extends EntityKeyframe<E>, E extends LivingEntity> extends EntityRecorder<K, E> {
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
