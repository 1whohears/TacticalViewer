package com.onewhohears.tacview.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccess {
    @Accessor("fallFlyTicks")
    void setFallFlyTicks(int fallFlyTicks);
}
