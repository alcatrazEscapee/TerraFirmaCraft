package net.dries007.tfc.common.blocks.crop;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import net.dries007.tfc.client.IHoeOverlayBlock;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.soil.IFarmlandBlock;
import net.dries007.tfc.common.blocks.soil.TFCFarmlandBlock;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.tileentity.CropTileEntity;
import net.dries007.tfc.common.tileentity.FarmlandTileEntity;
import net.dries007.tfc.common.types.Fertilizer;
import net.dries007.tfc.common.types.managers.FertilizerManager;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;


public abstract class CropBlock extends CropsBlock implements IHoeOverlayBlock
{
    public static final BooleanProperty WILD = TFCBlockStateProperties.WILD;

    public static final long DEFAULT_CROP_GROWTH_TICKS = ICalendar.TICKS_IN_DAY * 24;

    protected final FarmlandTileEntity.NutrientType primaryNutrient;
    protected final Supplier<? extends Block> dead;
    protected final Supplier<? extends Item> seeds;
    private final int maxAge;

    protected CropBlock(Properties properties, int maxAge, Supplier<? extends Block> dead, Supplier<? extends Item> seeds, FarmlandTileEntity.NutrientType primaryNutrient)
    {
        super(properties);

        this.maxAge = maxAge;
        this.dead = dead;
        this.seeds = seeds;
        this.primaryNutrient = primaryNutrient;

        registerDefaultState(defaultBlockState().setValue(getAgeProperty(), 0).setValue(WILD, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
    {
        return VoxelShapes.block(); // todo
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, IBlockReader worldIn, BlockPos pos)
    {
        return true; // todo
    }

    @Override
    public abstract IntegerProperty getAgeProperty();

    @Override
    public int getMaxAge()
    {
        return maxAge;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random)
    {
        tick(state, worldIn, pos, random);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit)
    {
        // Handle fertilizer applied directly to the crop
        if (!worldIn.isClientSide())
        {
            ItemStack stack = player.getItemInHand(handIn);
            Fertilizer fertilizer = FertilizerManager.get(stack);
            if (fertilizer != null)
            {
                FarmlandTileEntity farmland = Helpers.getTileEntity(worldIn, pos.below(), FarmlandTileEntity.class);
                if (farmland != null)
                {
                    farmland.addNutrients(fertilizer);
                    stack.shrink(1);
                    return ActionResultType.CONSUME;
                }
            }
        }
        return super.use(state, worldIn, pos, player, handIn, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand)
    {
        if (!worldIn.isClientSide())
        {
            if (canSurvive(state, worldIn, pos))
            {
                if (!state.getValue(WILD))
                {
                    growthTick(state, worldIn, pos, rand);
                }
            }
            else
            {
                // Cannot survive here (e.g. no farmland below)
                worldIn.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader worldIn, BlockPos pos)
    {
        final BlockState belowState = worldIn.getBlockState(pos.below());
        if (state.getValue(WILD))
        {
            return belowState.is(TFCTags.Blocks.WILD_CROP_GROWS_ON);
        }
        else
        {
            return belowState.getBlock() instanceof IFarmlandBlock;
        }
    }

    @Override
    protected IItemProvider getBaseSeedId()
    {
        return seeds.get();
    }

    @Override
    public boolean isBonemealSuccess(World worldIn, Random rand, BlockPos pos, BlockState state)
    {
        return false;
    }

    @Override
    public boolean isValidBonemealTarget(IBlockReader worldIn, BlockPos pos, BlockState state, boolean isClient)
    {
        return false;
    }

    @Override
    public void performBonemeal(ServerWorld worldIn, Random rand, BlockPos pos, BlockState state)
    {
        // No-op
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(getAgeProperty(), WILD);
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        return !state.getValue(WILD);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {
        return hasTileEntity(state) ? new CropTileEntity() : null;
    }

    @Override
    public void addHoeOverlayInfo(IWorld world, BlockPos pos, BlockState state, List<ITextComponent> lines)
    {
        BlockState belowState = world.getBlockState(pos.below());
        if (belowState.getBlock() instanceof IFarmlandBlock)
        {
            // Apply farmland tooltip after regular tooltip
            ((IFarmlandBlock) belowState.getBlock()).addHoeOverlayInfo(world, pos.below(), state, lines);
        }
    }

    protected void growthTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand)
    {
        // Non-wild crops must be growing on compatible farmland. Update the state of the below farmland first before doing any crop updates.
        final BlockPos belowPos = pos.below();
        final BlockState belowState = worldIn.getBlockState(belowPos);
        final CropTileEntity crop = Helpers.getTileEntityOrThrow(worldIn, pos, CropTileEntity.class);
        final IFarmlandBlock farmland = (IFarmlandBlock) belowState.getBlock();

        farmland.update(worldIn, belowPos, belowState);

        final int farmlandMoisture = farmland.getMoisture(worldIn, belowPos, belowState);
        final long tickDelta = Calendars.SERVER.getTicks() - crop.getLastTick();


        // Update the crop based on the last time it was ticked
        // todo: other checks here (temperature, mostly)
        if (canGrow(farmlandMoisture))
        {
            float growthDelta = crop.getGrowthVariation() * tickDelta / DEFAULT_CROP_GROWTH_TICKS;
            float yieldDelta = growthDelta * 0.25f; // Default yield modifier

            FarmlandTileEntity farmlandTileEntity = Helpers.getTileEntity(worldIn, belowPos, FarmlandTileEntity.class);
            if (farmlandTileEntity != null)
            {
                // Modifiers due to nutrients
                float availablePrimaryNutrient = farmlandTileEntity.getNutrient(primaryNutrient);
                float usedPrimaryNutrient = Math.min(growthDelta, availablePrimaryNutrient);
                float usedAllNutrients = farmlandTileEntity.consumeAll(growthDelta);

                // Bonus growth due to primary nutrient
                growthDelta += 0.5f * usedPrimaryNutrient;

                // Bonus yield due to all nutrients
                yieldDelta += 0.75f * 0.33f * usedAllNutrients;
            }

            float newGrowth = Math.min(1, crop.getGrowth() + growthDelta);
            float newYield = Math.min(1, crop.getYield() + yieldDelta);

            // Update the crop properties
            crop.setLastTick(Calendars.SERVER.getTicks());
            crop.setGrowth(newGrowth);
            crop.setYield(newYield);

            // Finally, update the current block state based on the growth
            int age = MathHelper.clamp((int) (newGrowth * getMaxAge()), 0, getMaxAge());
            if (age == getMaxAge())
            {
                onMature();
            }
            worldIn.setBlock(pos, state.setValue(getAgeProperty(), age), 3);
        }
        else if (canLive())
        {
            // Unable to grow, but still able to live
        }
        else
        {
            // Unable to live, so immediately die
        }
    }

    protected boolean canGrow(int farmlandMoisture)
    {
        return farmlandMoisture == TFCFarmlandBlock.FLOWING_MOISTURE;
    }

    protected boolean canLive()
    {
        return true;
    }

    protected void onMature()
    {

    }

    protected void onDeath()
    {

    }
}
