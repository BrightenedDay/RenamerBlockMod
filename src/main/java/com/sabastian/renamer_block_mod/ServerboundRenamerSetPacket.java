package com.sabastian.renamer_block_mod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

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

    public boolean handle(NetworkEvent.Context context) {

        if (context.getSender() == null)
            return false;

        context.enqueueWork(() -> {
            final BlockEntity blockEntity = context.getSender().level().getBlockEntity(this.blockPos);
            if (blockEntity instanceof RenamerBlockEntity renamer) {
                renamer.renameTo = renamerSet;
                renamer.getLevel().sendBlockUpdated(this.blockPos, renamer.getBlockState(), renamer.getBlockState(), Block.UPDATE_CLIENTS);
            }
        });

        context.setPacketHandled(true);
        return true;
    }
}