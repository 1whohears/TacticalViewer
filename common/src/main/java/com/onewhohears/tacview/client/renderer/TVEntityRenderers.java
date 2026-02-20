package com.onewhohears.tacview.client.renderer;

import com.onewhohears.tacview.init.TVModEntities;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;

public class TVEntityRenderers {

    public static void register() {
        EntityRendererRegistry.register(TVModEntities.TAC_VIEW, TacViewEntityRenderer::new);
    }

}
