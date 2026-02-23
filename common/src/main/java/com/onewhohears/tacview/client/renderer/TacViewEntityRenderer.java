package com.onewhohears.tacview.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onewhohears.tacview.client.core.TVClientManager;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TacViewEntityRenderer extends EntityRenderer<TacViewEntity> {

    public TacViewEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TacViewEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffer, int packedLight) {
        TVClientManager.get().renderTacViewEntity(entity, yaw, partialTick, stack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(TacViewEntity entity) {
        return null;
    }
}
