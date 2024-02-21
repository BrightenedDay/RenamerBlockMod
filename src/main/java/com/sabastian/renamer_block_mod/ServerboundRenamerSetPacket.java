package com.sabastian.renamer_block_mod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.security.cert.TrustAnchor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ServerboundRenamerSetPacket {

    private final String renamerSet;
    private final BlockPos blockPos;

    public ServerboundRenamerSetPacket(String pName, BlockPos blockPos) {
        this.renamerSet = pName;
        this.blockPos = blockPos;
    }

    public ServerboundRenamerSetPacket(FriendlyByteBuf buffer) {
        this.renamerSet = buffer.readUtf();
        this.blockPos = buffer.readBlockPos();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.renamerSet);
        buffer.writeBlockPos(blockPos);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {

        context.get().getSender().server.sendSystemMessage(Component.literal("Called"));

        if (context.get().getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.get().setPacketHandled(false);
            return;
        }

        context.get().enqueueWork(() -> {
            final BlockEntity blockEntity = context.get().getSender().level().getBlockEntity(this.blockPos);
            if (blockEntity instanceof RenamerBlockEntity renamer) {
                renamer.setRenameTo(this.renamerSet);
            }
        });
        context.get().setPacketHandled(true);
    }
}