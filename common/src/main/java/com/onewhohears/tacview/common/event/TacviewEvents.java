package com.onewhohears.tacview.common.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TacviewEvents {

    @Nullable
    public static Entity getPlayerVehicleToSave(@NotNull Entity player) {
        return GET_PLAYER_VEHICLE_TO_SAVE_EVENT.invoker().save(player);
    }

    public static final Event<GetPlayerVehicleToSave> GET_PLAYER_VEHICLE_TO_SAVE_EVENT =
            EventFactory.createLoop(GetPlayerVehicleToSave.class);

    @FunctionalInterface
    public interface GetPlayerVehicleToSave {
        @Nullable Entity save(@NotNull Entity player);
    }

}
