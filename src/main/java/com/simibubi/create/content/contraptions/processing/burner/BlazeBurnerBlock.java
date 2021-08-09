package com.simibubi.create.content.contraptions.processing.burner;

import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.jimfs.PathType;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.processing.BasinTileEntity;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.lib.annotation.MethodsReturnNonnullByDefault;
import com.simibubi.create.lib.entity.FakePlayer;
import com.simibubi.create.lib.extensions.BlockExtensions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.ConstantIntValue;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlazeBurnerBlock extends Block implements ITE<BlazeBurnerTileEntity>, EntityBlock, BlockExtensions {

	public static final EnumProperty<HeatLevel> HEAT_LEVEL = EnumProperty.create("blaze", HeatLevel.class);

	public BlazeBurnerBlock(Properties properties) {
		super(properties);
		registerDefaultState(super.defaultBlockState().setValue(HEAT_LEVEL, HeatLevel.NONE));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HEAT_LEVEL);
	}

	@Override
	public void onPlace(BlockState state, Level world, BlockPos pos, BlockState p_220082_4_, boolean p_220082_5_) {
		if (world.isClientSide)
			return;
		BlockEntity tileEntity = world.getBlockEntity(pos.above());
		if (!(tileEntity instanceof BasinTileEntity))
			return;
		BasinTileEntity basin = (BasinTileEntity) tileEntity;
		basin.notifyChangeOfContents();
	}

//	@Override
//	public boolean hasTileEntity(BlockState state) {
//		return state.getValue(HEAT_LEVEL)
//			.isAtLeast(HeatLevel.SMOULDERING);
//	}

	@Override
	public void fillItemCategory(CreativeModeTab p_149666_1_, NonNullList<ItemStack> p_149666_2_) {
		p_149666_2_.add(AllItems.EMPTY_BLAZE_BURNER.asStack());
		super.fillItemCategory(p_149666_1_, p_149666_2_);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockGetter world) {
		return AllTileEntities.HEATER.create();
	}

	@Override
	public Class<BlazeBurnerTileEntity> getTileEntityClass() {
		return BlazeBurnerTileEntity.class;
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult blockRayTraceResult) {
		ItemStack heldItem = player.getItemInHand(hand);
		boolean dontConsume = player.isCreative();
		boolean forceOverflow = !(player instanceof FakePlayer);

		if (!state.hasBlockEntity()) {
			if (heldItem.getItem() instanceof FlintAndSteelItem) {
				world.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
					world.random.nextFloat() * 0.4F + 0.8F);
				if (world.isClientSide)
					return InteractionResult.SUCCESS;
				heldItem.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
				world.setBlockAndUpdate(pos, AllBlocks.LIT_BLAZE_BURNER.getDefaultState());
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}

		boolean doNotConsume = player.isCreative();
		boolean forceOverflow = !(player instanceof FakePlayer);

		InteractionResultHolder<ItemStack> res = tryInsert(state, world, pos, heldItem, doNotConsume, forceOverflow, false);
		ItemStack leftover = res.getObject();
		if (!world.isClientSide && !doNotConsume && !leftover.isEmpty()) {
			if (heldItem.isEmpty()) {
				player.setItemInHand(hand, leftover);
			} else if (!player.inventory.add(leftover)) {
				player.drop(leftover, false);
			}
		}

		return res.getResult() == InteractionResult.SUCCESS ? res.getResult() : InteractionResult.PASS;
	}

	public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level world, BlockPos pos, ItemStack stack, boolean doNotConsume,
		boolean forceOverflow, boolean simulate) {
		if (!state.hasBlockEntity())
			return InteractionResultHolder.fail(ItemStack.EMPTY);

		BlockEntity te = world.getBlockEntity(pos);
		if (!(te instanceof BlazeBurnerTileEntity))
			return InteractionResultHolder.fail(ItemStack.EMPTY);
		BlazeBurnerTileEntity burnerTE = (BlazeBurnerTileEntity) te;

		if (burnerTE.isCreativeFuel(stack)) {
			if (!simulate)
				burnerTE.applyCreativeFuel();
			return ActionResult.success(ItemStack.EMPTY);
		}
		if (!burnerTE.tryUpdateFuel(stack, forceOverflow, simulate))
			return InteractionResultHolder.fail(ItemStack.EMPTY);

		if (!doNotConsume) {
			ItemStack container = stack.getContainerItem();
			if (!world.isClientSide && !simulate) {
				stack.shrink(1);
			}
			if (!container.isEmpty()) {
				return InteractionResultHolder.success(container);
			}
		}
		return InteractionResultHolder.success(ItemStack.EMPTY);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		ItemStack stack = context.getItemInHand();
		Item item = stack.getItem();
		BlockState defaultState = defaultBlockState();
		if (!(item instanceof BlazeBurnerBlockItem))
			return defaultState;
		HeatLevel initialHeat =
			((BlazeBurnerBlockItem) item).hasCapturedBlaze() ? HeatLevel.SMOULDERING : HeatLevel.NONE;
		return defaultState.setValue(HEAT_LEVEL, initialHeat);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
		return AllShapes.HEATER_BLOCK_SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockGetter p_220071_2_, BlockPos p_220071_3_,
		CollisionContext p_220071_4_) {
		if (p_220071_4_ == CollisionContext.empty())
			return AllShapes.HEATER_BLOCK_SPECIAL_COLLISION_SHAPE;
		return getShape(p_220071_1_, p_220071_2_, p_220071_3_, p_220071_4_);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState p_149740_1_) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level p_180641_2_, BlockPos p_180641_3_) {
		return Math.max(0, state.getValue(HEAT_LEVEL).ordinal() - 1);
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathType type) {
		return false;
	}

	@Environment(EnvType.CLIENT)
	public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
		if (random.nextInt(10) != 0)
			return;
		if (!state.getValue(HEAT_LEVEL)
			.isAtLeast(HeatLevel.SMOULDERING))
			return;
		world.playLocalSound((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F),
			(double) ((float) pos.getZ() + 0.5F), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
			0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
	}

	public static HeatLevel getHeatLevelOf(BlockState blockState) {
		return blockState.hasProperty(BlazeBurnerBlock.HEAT_LEVEL) ? blockState.getValue(BlazeBurnerBlock.HEAT_LEVEL)
			: HeatLevel.NONE;
	}

	public static int getLight(BlockState state) {
		return MathHelper.clamp(state.getValue(HEAT_LEVEL)
			.ordinal() * 4 - 1, 0, 15);
	}

	public static LootTable.Builder buildLootTable() {
		IBuilder survivesExplosion = SurvivesExplosion.survivesExplosion();
		BlazeBurnerBlock block = AllBlocks.BLAZE_BURNER.get();

		LootTable.Builder builder = LootTable.lootTable();
		LootPool.Builder poolBuilder = LootPool.lootPool();
		for (HeatLevel level : HeatLevel.values()) {
			IItemProvider drop =
				level == HeatLevel.NONE ? AllItems.EMPTY_BLAZE_BURNER.get() : AllBlocks.BLAZE_BURNER.get();
			poolBuilder.add(ItemLootEntry.lootTableItem(drop)
				.when(survivesExplosion)
				.when(BlockStateProperty.hasBlockStateProperties(block)
					.setProperties(StatePropertiesPredicate.Builder.properties()
						.hasProperty(HEAT_LEVEL, level))));
		}
		builder.withPool(poolBuilder.setRolls(ConstantRange.exactly(1)));
		return builder;
	}

	public enum HeatLevel implements StringRepresentable {
		NONE, SMOULDERING, FADING, KINDLED, SEETHING, ;

		public static HeatLevel byIndex(int index) {
			return values()[index];
		}

		public HeatLevel nextActiveLevel() {
			return byIndex(ordinal() % (values().length - 1) + 1);
		}

		public boolean isAtLeast(HeatLevel heatLevel) {
			return this.ordinal() >= heatLevel.ordinal();
		}

		@Override
		public String getSerializedName() {
			return Lang.asId(name());
		}
	}

	/**
	 * @author Platymemo
	 * Replaces the need for {@link BlazeBurnerHandler}
	 */
	@Override
	public void onProjectileHit(Level world, BlockState blockState, BlockHitResult blockRayTraceResult, Projectile projectileEntity) {
		super.onProjectileHit(world, blockState, blockRayTraceResult, projectileEntity);

		if (!(projectileEntity instanceof ThrownEgg))
			return;

		BlockEntity tile = world.getBlockEntity(blockRayTraceResult.getBlockPos());
		if (!(tile instanceof BlazeBurnerTileEntity)) {
			return;
		}

		projectileEntity.setDeltaMovement(Vec3.ZERO);
		projectileEntity.remove();

		BlazeBurnerTileEntity heater = (BlazeBurnerTileEntity) tile;
		if (heater.activeFuel != BlazeBurnerTileEntity.FuelType.SPECIAL) {
			heater.activeFuel = BlazeBurnerTileEntity.FuelType.NORMAL;
			heater.remainingBurnTime =
					Mth.clamp(heater.remainingBurnTime + 80, 0, BlazeBurnerTileEntity.maxHeatCapacity);
			heater.updateBlockState();
			heater.notifyUpdate();
		}
		AllSoundEvents.BLAZE_MUNCH.playOnServer(world, heater.getBlockPos());
	}
}
