package com.onewhohears.tacview.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;

import static com.onewhohears.tacview.TacViewMod.MOD_ID;

public class TVKeyBinds {

    public static KeyMapping PAUSE, FORWARD_TEN, BACKWARD_TEN, FORWARD_TICK, BACKWARD_TICK;

    public static void init() {
        PAUSE = registerKey("pause", InputConstants.UNKNOWN, "key.categories.tacview");
        FORWARD_TEN = registerKey("forward_ten", InputConstants.UNKNOWN, "key.categories.tacview");
        BACKWARD_TEN = registerKey("backward_ten", InputConstants.UNKNOWN, "key.categories.tacview");
        FORWARD_TICK = registerKey("forward_tick", InputConstants.UNKNOWN, "key.categories.tacview");
        BACKWARD_TICK = registerKey("backward_tick", InputConstants.UNKNOWN, "key.categories.tacview");
    }

    private static KeyMapping registerKey(String name, InputConstants.Key key, String category) {
        KeyMapping mapping = new KeyMapping("key."+MOD_ID+"."+name, key.getValue(), category);
        KeyMappingRegistry.register(mapping);
        return mapping;
    }

}
