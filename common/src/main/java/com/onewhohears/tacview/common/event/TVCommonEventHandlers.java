package com.onewhohears.tacview.common.event;

import com.mojang.brigadier.CommandDispatcher;
import com.onewhohears.tacview.common.command.TacViewCommands;
import com.onewhohears.tacview.common.core.SaveStateManager;
import com.onewhohears.tacview.common.core.SessionManager;
import com.onewhohears.tacview.common.core.recordevent.MoreRecordEvents;
import com.onewhohears.tacview.common.network.TVPacketHandler;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

public class TVCommonEventHandlers {

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(TVCommonEventHandlers::onServerLevelPost);
        CommandRegistrationEvent.EVENT.register(TVCommonEventHandlers::registerCommands);
        LifecycleEvent.SETUP.register(TVCommonEventHandlers::onSetup);
        PlayerEvent.ATTACK_ENTITY.register(TVCommonEventHandlers::onPlayerAttackEntity);
        EntityEvent.LIVING_DEATH.register(TVCommonEventHandlers::onLivingDeath);
    }

    private static EventResult onLivingDeath(LivingEntity entity, DamageSource source) {
        SessionManager.get().recordEvent(new MoreRecordEvents.LivingDeath(entity), true, entity);
        return EventResult.pass();
    }

    private static EventResult onPlayerAttackEntity(Player player, Level level, Entity entity,
                                                    InteractionHand hand, @Nullable EntityHitResult hit) {
        SessionManager.get().recordEvent(new MoreRecordEvents.PlayerAttack(player, entity), true, player, entity);
        return EventResult.pass();
    }

    private static void onSetup() {
        TVPacketHandler.register();
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                         CommandBuildContext context,
                                         Commands.CommandSelection selection) {
        new TacViewCommands(dispatcher);
    }

    private static void onServerLevelPost(ServerLevel level) {
        SessionManager.get().tickRecord(level);
        SaveStateManager.get().tick(level);
    }

}
