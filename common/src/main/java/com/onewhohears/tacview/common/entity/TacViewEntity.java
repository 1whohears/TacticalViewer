package com.onewhohears.tacview.common.entity;

import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.tacview.client.core.ClientPlayback;
import com.onewhohears.tacview.common.core.RecordingSession;
import com.onewhohears.tacview.common.core.SessionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class TacViewEntity extends Entity {

    public static final EntityDataAccessor<Boolean> PAUSED = SynchedEntityData.defineId(TacViewEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Long> TICK = SynchedEntityData.defineId(TacViewEntity.class, EntityDataSerializers.LONG);
    public static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(TacViewEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(TacViewEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> SESSION_ID = SynchedEntityData.defineId(TacViewEntity.class, EntityDataSerializers.STRING);

    /**
     * NULL IF ON SERVER
     */
    public final ClientPlayback playback;

    @Override
    public void tick() {
        super.tick();
        RecordingSession session = SessionManager.get().getSession(getSessionId());
        controlTime(session);
    }

    protected void controlTime(@Nullable RecordingSession session) {
        if (isPaused()) {
            fixTick(session);
            return;
        }
        tickStep(getTickRate());
        fixTick(session);
    }

    protected void fixTick(@Nullable RecordingSession session) {
        if (session == null) return;
        long tick = getPlaybackTick();
        long start = session.getSessionStartTime();
        if (tick < start) {
            setPlaybackTick(start);
            return;
        }
        long end = start + session.getLength();
        if (tick > end) setPlaybackTick(end);
    }

    public void tickStep(int steps) {
        setPlaybackTick(getPlaybackTick() + steps);
    }

    public TacViewEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        if (UtilEntity.getLevel(this).isClientSide()) playback = new ClientPlayback(this);
        else playback = null;
        blocksBuilding = true;
        noPhysics = true;
        noCulling = true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        setPaused(nbt.getBoolean("paused"));
        setPlaybackTick(nbt.getLong("playback_tick"));
        setWidth(nbt.getFloat("width"));
        setHeight(nbt.getFloat("height"));
        setSessionId(nbt.getString("session_id"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putBoolean("paused", isPaused());
        nbt.putLong("playback_tick", getPlaybackTick());
        nbt.putFloat("width", getWidth());
        nbt.putFloat("height", getHeight());
        nbt.putString("session_id", getSessionId());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(PAUSED, false);
        entityData.define(TICK, 0L);
        entityData.define(WIDTH, 4F);
        entityData.define(HEIGHT, 4F);
        entityData.define(SESSION_ID, "");
    }

    public boolean isPaused() {
        return entityData.get(PAUSED);
    }

    public void setPaused(boolean paused) {
        entityData.set(PAUSED, paused);
    }

    public long getPlaybackTick() {
        return entityData.get(TICK);
    }

    public void setPlaybackTick(long tick) {
        entityData.set(TICK, tick);
    }

    public String getSessionId() {
        return entityData.get(SESSION_ID);
    }

    public void setSessionId(String sessionId) {
        entityData.set(SESSION_ID, sessionId);
    }

    public boolean hasSession() {
        return !getSessionId().isEmpty();
    }

    public int getTickRate() {
        return 1; // TODO variable tick rate
    }

    public float getWidth() {
        return entityData.get(WIDTH);
    }

    public float getHeight() {
        return entityData.get(HEIGHT);
    }

    public void setWidth(float width) {
        entityData.set(WIDTH, width);
    }

    public void setHeight(float height) {
        entityData.set(HEIGHT, height);
    }

    public void resetReplay() {
        setPlaybackTick(0);
        setPaused(false);
    }
}
