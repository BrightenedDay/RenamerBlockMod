package com.sabastian.renamer_block_mod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenamerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer
{
    private final ItemStackHandler itemHandler = new ItemStackHandler(2);

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private String renameTo = "";

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    public RenamerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(RenamerBlockMod.RENAMER_BLOCKENTITY.get(), pPos, pBlockState);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        //lazyItemHandler = SidedInvWrapper.create(this, new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH})
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.renamer_block_mod.renamer_block");
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

    public void setRenameTo(String newRename) {
        this.renameTo = newRename;
    }

    public String getRename() {
        return renameTo;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {

        if (this.renameTo == null)
            return;

        if (!this.itemHandler.getStackInSlot(INPUT_SLOT).isEmpty() && AvailableToOutput()) {
            setChanged(level, pos, state);
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
            }
        }
    }

    private boolean AvailableToOutput() {
        if (this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() < this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize()) {
            if (!this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
                return this.itemHandler.getStackInSlot(INPUT_SLOT).is(this.itemHandler.getStackInSlot(OUTPUT_SLOT).getItem());
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("inv", this.itemHandler.serializeNBT());
        pTag.putString("restring", RenamerBlockEntity.this.renameTo);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.renameTo = pTag.getString("restring");
        this.itemHandler.deserializeNBT(pTag.getCompound("inv"));
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
        if (direction != Direction.DOWN)
            return true;
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        if (direction == Direction.DOWN)
            return true;
        return false;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
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
        return null;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {

    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
    }
}