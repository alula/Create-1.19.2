package com.simibubi.create.foundation.utility.recipe;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public class DummyCraftingContainer extends CraftingContainer {

	private final NonNullList<ItemStack> inv;

	public DummyCraftingContainer(Storage<ItemVariant> itemHandler) {
		super(null, 0, 0);

		this.inv = createInventory(itemHandler);
	}

	@Override
	public int getContainerSize() {
		return this.inv.size();
	}

	@Override
	public boolean isEmpty() {
		for (int slot = 0; slot < this.getContainerSize(); slot++) {
			if (!this.getItem(slot).isEmpty())
				return false;
		}

		return true;
	}

	@Override
	public @NotNull ItemStack getItem(int slot) {
		return slot >= this.getContainerSize() ? ItemStack.EMPTY : this.inv.get(slot);
	}

	@Override
	public @NotNull ItemStack removeItemNoUpdate(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public @NotNull ItemStack removeItem(int slot, int count) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, @NotNull ItemStack stack) {}

	@Override
	public void clearContent() {}

	@Override
	public void fillStackedContents(@NotNull StackedContents helper) {}

	private static NonNullList<ItemStack> createInventory(Storage<ItemVariant> itemHandler) {
		try (Transaction t = TransferUtil.getTransaction()) {
			List<ItemStack> stacks = TransferUtil.extractAllAsStacks(itemHandler);
			return NonNullList.of(null, stacks.toArray(ItemStack[]::new));
		}
	}

}
