package com.onewhohears.tacview.common.network.toserver;

import com.onewhohears.onewholibs.util.UtilEntity;
import com.onewhohears.tacview.client.core.ViewerInputs;
import com.onewhohears.tacview.common.entity.TacViewEntity;
import com.onewhohears.tacview.common.network.TVPacketHandler;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ToServerUpdateViewer extends BaseC2SMessage {

    public final int viewerId;
    public final ViewerInputs input;

    public ToServerUpdateViewer(@NotNull TacViewEntity viewer, @NotNull ViewerInputs input) {
        this.viewerId = viewer.getId();
        this.input = input;
    }

    public ToServerUpdateViewer(FriendlyByteBuf buffer) {
        this.viewerId = buffer.readInt();
        this.input = buffer.readEnum(ViewerInputs.class);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(viewerId);
        buffer.writeEnum(input);
    }

    @Override
    public void handle(NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            if (ctx.getEnvironment().toPlatform() != EnvType.SERVER) return;
            Level level = UtilEntity.getLevel(ctx.getPlayer());
            if (!(level.getEntity(viewerId) instanceof TacViewEntity viewer)) return;
            viewer.handleInput(input);
        });
    }

    @Override
    public MessageType getType() {
        return TVPacketHandler.C2S_UPDATE_VIEWER;
    }
}
