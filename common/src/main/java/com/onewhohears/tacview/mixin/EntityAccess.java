package com.onewhohears.tacview.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccess {
    @Invoker(value = "setSharedFlag")
    void invokeSetSharedFlag(int id, boolean flag);
}
