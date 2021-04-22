package com.simibubi.create.content.logistics.block.inventories;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.lib.annotation.MethodsReturnNonnullByDefault;
import com.simibubi.create.lib.lba.ItemHandlerHelper;
import com.simibubi.create.lib.lba.ItemStackHandler;

import net.minecraft.item.ItemStack;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BottomlessItemHandler extends ItemStackHandler {

	private Supplier<ItemStack> suppliedItemStack;

	public BottomlessItemHandler(Supplier<ItemStack> suppliedItemStack) {
		this.suppliedItemStack = suppliedItemStack;
	}

	@Override
	public int getSlots() {
		return 2;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		ItemStack stack = suppliedItemStack.get();
		if (slot == 1)
			return ItemStack.EMPTY;
		if (stack == null)
			return ItemStack.EMPTY;
		if (!stack.isEmpty())
			return ItemHandlerHelper.copyStackWithSize(stack, stack.getMaxStackSize());
		return stack;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack stack = suppliedItemStack.get();
		if (slot == 1)
			return ItemStack.EMPTY;
		if (stack == null)
			return ItemStack.EMPTY;
		if (!stack.isEmpty())
			return ItemHandlerHelper.copyStackWithSize(stack, Math.min(stack.getMaxStackSize(), amount));
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}
}
