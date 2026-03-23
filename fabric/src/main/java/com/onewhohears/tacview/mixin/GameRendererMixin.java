package com.onewhohears.tacview.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onewhohears.tacview.client.core.TVClientManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Final @Shadow
    private Camera mainCamera;

    @Inject(
            method = "renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void dscombat_fabric_cameraAngleSetup(float f, long l, PoseStack poseStack, CallbackInfo ci) {
        TVClientManager.get().playerLookAtTrackedEntity(mainCamera.getPosition(), (xRot, yRot) -> {
            mainCamera.xRot = xRot;
            mainCamera.yRot = yRot;
            Minecraft m = Minecraft.getInstance();
            m.player.setXRot(xRot);
            m.player.setYRot(yRot);
        });
    }
}
