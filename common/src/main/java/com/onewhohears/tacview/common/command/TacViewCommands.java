package com.onewhohears.tacview.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.onewhohears.onewholibs.util.UtilMCText;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TacViewCommands {

    public TacViewCommands(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tacview").requires((stack) -> stack.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("session_id", StringArgumentType.word()) // TODO auto name new recordings argument
                                .suggests(suggestLoadedSessionId())
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
                                .suggests(suggestLoadedSessionId())
                                .executes(ctx -> stopRecording(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "session_id")
                                ))
                        )
                )
                .then(Commands.literal("info")
                        .executes(ctx -> listInfoAll(ctx.getSource()))
                        .then(Commands.argument("session_id", StringArgumentType.word())
                                .suggests(suggestLoadedSessionId())
                                .executes(ctx -> listLoadedInfo(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "session_id")
                                ))
                        )
                )
                .then(Commands.literal("load")
                        .then(Commands.argument("session_id", StringArgumentType.word())
                                .suggests(suggestUnloadedSessionId())
                                .executes(ctx -> loadRecording(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "session_id")
                                ))
                        )
                )
                .then(Commands.literal("watch")
                        .then(Commands.argument("session_id", StringArgumentType.word())
                                .suggests(suggestLoadedSessionId())
                                .executes(ctx -> watchReplay(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "session_id"), null
                                ))
                                .then(Commands.argument("viewer_entity", EntityArgument.entity())
                                        .executes(ctx -> watchReplay(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "session_id"),
                                                EntityArgument.getEntity(ctx, "viewer_entity")
                                                        instanceof TacViewEntity viewer ? viewer : null
                                        ))
                                )
                        )
                )
                // TODO pause command
                // TODO unpause command
                // TODO restart command
                // TODO step (+/- ticks) command
                .then(Commands.literal("create_viewer")
                        .executes(ctx -> createViewer(ctx.getSource(),
                                null, -1, -1
                        ))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> createViewer(ctx.getSource(),
                                        Vec3Argument.getVec3(ctx, "pos"), -1, -1
                                ))
                                .then(Commands.argument("width", FloatArgumentType.floatArg(1, 100))
                                        .executes(ctx -> createViewer(ctx.getSource(),
                                                Vec3Argument.getVec3(ctx, "pos"),
                                                FloatArgumentType.getFloat(ctx, "width"), -1
                                        ))
                                        .then(Commands.argument("height", FloatArgumentType.floatArg(1, 100))
                                                .executes(ctx -> createViewer(ctx.getSource(),
                                                        Vec3Argument.getVec3(ctx, "pos"),
                                                        FloatArgumentType.getFloat(ctx, "width"),
                                                        FloatArgumentType.getFloat(ctx, "height")
                                                ))
                                        )
                                )
                        )
                )
        );
    }

    private SuggestionProvider<CommandSourceStack> suggestLoadedSessionId() {
        return (context, builder) -> {
            SessionManager.get().getLoadedSessionIds().forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSourceStack> suggestUnloadedSessionId() {
        return (context, builder) -> {
            SessionManager.get().getUnloadedSessionIds().forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private int createViewer(@NotNull CommandSourceStack source, @Nullable Vec3 pos, float width, float height) {
        if (pos == null) pos = source.getPosition();
        if (width == -1) width = 4;
        if (height == -1) height = 4;
        AtomicReference<String> msg = new AtomicReference<>();
        boolean result = SessionManager.get().createViewer(source.getLevel(), pos, width, height, msg::set);
        if (result) source.sendSuccess(() -> UtilMCText.literal(msg.get()), true);
        else source.sendFailure(UtilMCText.literal(msg.get()));
        return result ? 1 : 0;
    }

    private int watchReplay(@NotNull CommandSourceStack source, @NotNull String sessionId,
                            @Nullable TacViewEntity entity) {
        if (entity == null) {
            AABB aabb = AABB.ofSize(source.getPosition(), 1, 1, 1).inflate(32);
            List<TacViewEntity> viewers = source.getLevel().getEntitiesOfClass(TacViewEntity.class, aabb);
            if (viewers.isEmpty()) {
                source.sendFailure(UtilMCText.literal("No viewer entities within 32 blocks found"));
                return 0;
            }
            entity = findClosestEntity(source.getPosition(), viewers);
        }
        AtomicReference<String> msg = new AtomicReference<>();
        boolean result = SessionManager.get().watchReplay(entity, sessionId, msg::set);
        if (result) source.sendSuccess(() -> UtilMCText.literal(msg.get()), true);
        else source.sendFailure(UtilMCText.literal(msg.get()));
        return result ? 1 : 0;
    }

    private static TacViewEntity findClosestEntity(Vec3 pos, List<TacViewEntity> entities) {
        TacViewEntity closestEntity = null;
        double minDistance = Double.MAX_VALUE;
        for (TacViewEntity entity : entities) {
            double distance = pos.distanceToSqr(entity.position());
            if (distance < minDistance) {
                minDistance = distance;
                closestEntity = entity;
            }
        }
        return closestEntity;
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
        return result ? 1 : 0;
    }

    private int loadRecording(@NotNull CommandSourceStack source, @NotNull String sessionId) {
        AtomicReference<String> msg = new AtomicReference<>();
        boolean result = SessionManager.get().readSessionData(sessionId, msg::set);
        if (result) source.sendSuccess(() -> UtilMCText.literal(msg.get()), true);
        else source.sendFailure(UtilMCText.literal(msg.get()));
        return result ? 1 : 0;
    }

    private int startRecording(@NotNull CommandSourceStack source, @NotNull String sessionId,
                               @NotNull Collection<? extends Entity> entities,
                               int maxRecordTicks, int recordRate) {
        AtomicReference<String> msg = new AtomicReference<>();
        boolean result = SessionManager.get().startNewSession(sessionId, entities, source.getLevel(),
                recordRate, maxRecordTicks, msg::set);
        if (result) source.sendSuccess(() -> UtilMCText.literal(msg.get()), true);
        else source.sendFailure(UtilMCText.literal(msg.get()));
        return result ? 1 : 0;
    }

}
