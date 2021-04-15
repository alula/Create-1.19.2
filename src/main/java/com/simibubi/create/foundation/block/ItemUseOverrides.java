package com.simibubi.create.foundation.block;

import java.util.HashSet;
import java.util.Set;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;

public class ItemUseOverrides {

	private static final Set<ResourceLocation> overrides = new HashSet<>();

	public static void addBlock(Block block) {
		overrides.add(block.getRegistryName());
	}

	public static void onBlockActivated(PlayerInteractEvent.RightClickBlock event) {
		if (AllItems.WRENCH.isIn(event.getItemStack()))
			return;

		BlockState state = event.getWorld()
			.getBlockState(event.getPos());
		ResourceLocation id = state.getBlock()
			.getRegistryName();

		if (!overrides.contains(id))
			return;

		BlockRayTraceResult blockTrace =
			new BlockRayTraceResult(VecHelper.getCenterOf(event.getPos()), event.getFace(), event.getPos(), true);
		ActionResultType result = state.onUse(event.getWorld(), event.getPlayer(), event.getHand(), blockTrace);

		if (!result.isAccepted())
			return;

		event.setCanceled(true);
		event.setCancellationResult(result);

	}
}
