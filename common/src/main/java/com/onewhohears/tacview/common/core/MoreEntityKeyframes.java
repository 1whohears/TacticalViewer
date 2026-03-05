package com.onewhohears.tacview.common.core;

import com.google.gson.JsonObject;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class MoreEntityKeyframes {
    public static abstract class AbstractLivingEntityKeyframe<E extends LivingEntity> extends EntityKeyframe<E> {
        public final KeyframeValue.FloatV<E> health = registerFloatValue("health", LivingEntity::getHealth, LivingEntity::setHealth);
        public final KeyframeValue.EnumV<Pose,E> pose = registerEnumValue("pose", Entity::getPose, Entity::setPose, Pose.class);
        public final KeyframeValue.ItemStackV<E> mainHand = registerItemStackValue("mainHand", LivingEntity::getMainHandItem,
                (entity, item) -> entity.setItemInHand(InteractionHand.MAIN_HAND, item));
        public final KeyframeValue.ItemStackV<E> offHand = registerItemStackValue("offHand", LivingEntity::getOffhandItem,
                (entity, item) -> entity.setItemInHand(InteractionHand.OFF_HAND, item));
        // TODO fallFlying
        protected AbstractLivingEntityKeyframe() {
            super();
        }
        public AbstractLivingEntityKeyframe(@NotNull E entity) {
            super(entity);
        }
        public AbstractLivingEntityKeyframe(@NotNull JsonObject data) {
            super(data);
        }
    }
    public static class LivingEntityKeyframe extends AbstractLivingEntityKeyframe<LivingEntity> {
        protected LivingEntityKeyframe() {
            super();
        }
        public LivingEntityKeyframe(@NotNull LivingEntity entity) {
            super(entity);
        }
        public LivingEntityKeyframe(@NotNull JsonObject data) {
            super(data);
        }
    }
    public static class PlayerKeyframe extends AbstractLivingEntityKeyframe<Player> {
        // TODO armor slots
        protected PlayerKeyframe() {
            super();
        }
        public PlayerKeyframe(@NotNull Player entity) {
            super(entity);
        }
        public PlayerKeyframe(@NotNull JsonObject data) {
            super(data);
        }
    }
}
