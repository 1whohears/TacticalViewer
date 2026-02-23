package com.onewhohears.tacview.common.event;

import com.mojang.brigadier.CommandDispatcher;
import com.onewhohears.tacview.common.command.TacViewCommands;
import com.onewhohears.tacview.common.core.SessionManager;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;

public class TVCommonEventHandlers {

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(TVCommonEventHandlers::onServerLevelPost);
        CommandRegistrationEvent.EVENT.register(TVCommonEventHandlers::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                         CommandBuildContext context,
                                         Commands.CommandSelection selection) {
        new TacViewCommands(dispatcher);
    }

    private static void onServerLevelPost(ServerLevel level) {
        SessionManager.get().tickRecord(level);
    }

}
