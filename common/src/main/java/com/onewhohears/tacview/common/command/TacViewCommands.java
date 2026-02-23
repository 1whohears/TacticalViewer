package com.onewhohears.tacview.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.onewhohears.onewholibs.util.UtilMCText;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class TacViewCommands {

    public TacViewCommands(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tacview").requires((stack) -> stack.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("session_id", StringArgumentType.word())
                                .then(Commands.argument("tracked_entities", EntityArgument.entities())
                                        .executes(ctx -> startRecording(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "session_id"),
                                                EntityArgument.getEntities(ctx, "tracked_entities"),
                                                1200, 5
                                        ))
                                        .then(Commands.argument("max_record_ticks", IntegerArgumentType.integer(1))
                                                .executes(ctx -> startRecording(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "session_id"),
                                                        EntityArgument.getEntities(ctx, "tracked_entities"),
                                                        IntegerArgumentType.getInteger(ctx, "max_record_ticks"),
                                                        5
                                                ))
                                                .then(Commands.argument("record_rate_ticks", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> startRecording(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "session_id"),
                                                                EntityArgument.getEntities(ctx, "tracked_entities"),
                                                                IntegerArgumentType.getInteger(ctx, "max_record_ticks"),
                                                                IntegerArgumentType.getInteger(ctx, "record_rate_ticks")
                                                        ))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("stop")
                        .then(Commands.argument("session_id", StringArgumentType.word())
                                .executes(ctx -> stopRecording(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "session_id")
                                ))
                        )
                )
                .then(Commands.literal("info")
                        .executes(ctx -> listInfoAll(ctx.getSource()))
                        .then(Commands.argument("session_id", StringArgumentType.word())
                                .executes(ctx -> listLoadedInfo(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "session_id")
                                ))
                        )
                )
        );
    }

    private int listLoadedInfo(@NotNull CommandSourceStack source, @NotNull String sessionId) {
        RecordingSession session = SessionManager.get().getSession(sessionId);
        if (session == null) {
            source.sendFailure(UtilMCText.literal("Recording "+sessionId+" does not exist or is not loaded!"));
            return 0;
        }
        source.sendSystemMessage(UtilMCText.literal(session.toString()));
        return 1;
    }

    private int listInfoAll(@NotNull CommandSourceStack source) {
        for (String id : SessionManager.get().getLoadedSessionIds()) listLoadedInfo(source, id);
        StringBuilder unloadedSessions = new StringBuilder("Unloaded Recording: ");
        for (String id : SessionManager.get().getUnloadedSessionIds()) unloadedSessions.append(id).append(", ");
        source.sendSystemMessage(UtilMCText.literal(unloadedSessions.toString()));
        return 1;
    }

    private int stopRecording(@NotNull CommandSourceStack source, @NotNull String sessionId) {
        AtomicReference<String> msg = new AtomicReference<>();
        boolean result = SessionManager.get().stopRecording(sessionId, source.getLevel(), msg::set);
        if (result) source.sendSuccess(() -> UtilMCText.literal(msg.get()), true);
        else source.sendFailure(UtilMCText.literal(msg.get()));
        return result ? 1: 0;
    }

    private int startRecording(@NotNull CommandSourceStack source, @NotNull String sessionId,
                               @NotNull Collection<? extends Entity> entities,
                               int maxRecordTicks, int recordRate) {
        AtomicReference<String> msg = new AtomicReference<>();
        boolean result = SessionManager.get().startNewSession(sessionId, entities, source.getLevel(),
                recordRate, maxRecordTicks, msg::set);
        if (result) source.sendSuccess(() -> UtilMCText.literal(msg.get()), true);
        else source.sendFailure(UtilMCText.literal(msg.get()));
        return result ? 1: 0;
    }

}
