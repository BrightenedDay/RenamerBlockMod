package com.sabastian.renamer_block_mod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenamerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, Nameable
{
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private LazyOptional<? extends IItemHandler>[] handlers;

    //private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    public String renameTo = "";
    @Nullable
    private Component name;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    public RenamerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(RenamerBlockMod.RENAMER_BLOCKENTITY.get(), pPos, pBlockState);
        this.handlers = SidedInvWrapper.create(this, new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH});
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!this.remove && cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) {
                return handlers[OUTPUT_SLOT].cast();
            } else {
                return handlers[INPUT_SLOT].cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlers[0].invalidate();
        handlers[1].invalidate();
        //lazyItemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.handlers = SidedInvWrapper.create(this, new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH});
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    public void setCustomName(Component pName) {
        this.name = pName;
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : Component.translatable("block.renamer_block_mod.renamer_block");
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new RenamerMenu(i, inventory, this);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {

        if (level == null || this.level.isClientSide())
            return;

        if (this.renameTo == null)
            return;

        if (!this.itemHandler.getStackInSlot(INPUT_SLOT).isEmpty() && AvailableToOutput()) {
            ItemStack itemStack = this.itemHandler.getStackInSlot(INPUT_SLOT).copy();
            int leftSpace = itemStack.getMaxStackSize() - this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount();
            int extractAmount = 0;

            if (leftSpace >= itemStack.getCount()) {
                extractAmount = itemStack.getCount();
            } else {
                extractAmount = leftSpace;
            }

            if (extractAmount > 0) {
                this.itemHandler.extractItem(INPUT_SLOT, extractAmount, false);
                itemStack.setCount(extractAmount);

                if (this.renameTo.trim().isEmpty()) {
                    itemStack.resetHoverName();
                } else {
                    itemStack.setHoverName(Component.literal(this.renameTo));
                }

                this.itemHandler.insertItem(OUTPUT_SLOT, itemStack, false);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
        }
    }

    private boolean AvailableToOutput() {

        if (this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty())
            return true;

        if (this.itemHandler.getStackInSlot(INPUT_SLOT).is(this.itemHandler.getStackInSlot(OUTPUT_SLOT).getItem())) {
            return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() < this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
        }

        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("inv", this.itemHandler.serializeNBT());
        pTag.putString("restring", this.renameTo);
        if (this.name != null) {
            pTag.putString("CustomName", Component.Serializer.toJson(this.name));
        }
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.renameTo = pTag.getString("restring");
        if (pTag.contains("inv"))
            this.itemHandler.deserializeNBT(pTag.getCompound("inv"));
        if (pTag.contains("CustomName", 8)) {
            this.name = Component.Serializer.fromJson(pTag.getString("CustomName"));
        }
    }

    @Override
    @NotNull
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("inv", this.itemHandler.serializeNBT());
        tag.putString("restring", this.renameTo);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN) {
            return new int[0];
        } else {
            return new int[1];
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        return this.canPlaceItem(i, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return this.itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {

        if (itemHandler.getStackInSlot(INPUT_SLOT) == ItemStack.EMPTY && itemHandler.getStackInSlot(OUTPUT_SLOT) == ItemStack.EMPTY)
            return true;

        return false;
    }

    @Override
    public ItemStack getItem(int i) {
        return this.itemHandler.getStackInSlot(i);
    }

    @Override
    public ItemStack removeItem(int i, int a) {
        return i >= 0 && i < this.itemHandler.getStackInSlot(i).getCount() && !(this.itemHandler.getStackInSlot(i)).isEmpty() && a > 0 ? (this.itemHandler.getStackInSlot(i)).split(a) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        if (i >= 0 && i < this.itemHandler.getSlots()){
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            this.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.itemHandler.setStackInSlot(i, itemStack);
        if (itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        this.itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
    }
}