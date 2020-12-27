package net.dries007.tfc.common.blocks.crop;

import java.util.Random;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import net.minecraftforge.common.Tags;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.IFarmlandBlock;
import net.dries007.tfc.common.blocks.soil.TFCFarmlandBlock;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.tileentity.CropTileEntity;
import net.dries007.tfc.common.tileentity.FarmlandTileEntity;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;

public abstract class DoubleCropBlock extends CropBlock
{
    public static final EnumProperty<Part> PART = TFCBlockStateProperties.DOUBLE_CROP_PART;

    public static DoubleCropBlock create(Properties properties, int singleStages, int doubleStages, Crop crop)
    {
        IntegerProperty property = TFCBlockStateProperties.getAgeProperty(singleStages + doubleStages - 1);
        return new DoubleCropBlock(properties, singleStages - 1, singleStages + doubleStages - 1, TFCBlocks.DEAD_CROPS.get(crop), TFCItems.CROP_SEEDS.get(crop), crop.getPrimaryNutrient())
        {
            @Override
            public IntegerProperty getAgeProperty()
            {
                return property;
            }
        };
    }

    /**
     * Modified from {@link net.minecraft.block.DoublePlantBlock#preventCreativeDropFromBottomPart(World, BlockPos, BlockState, PlayerEntity)}
     */
    public static void preventCreativeDropFromBottomPart(World worldIn, BlockPos pos, BlockState state, PlayerEntity player)
    {
        Part part = state.getValue(PART);
        if (part == Part.TOP)
        {
            final BlockPos belowPos = pos.below();
            final BlockState belowState = worldIn.getBlockState(belowPos);
            if (belowState.getBlock() == state.getBlock() && belowState.getValue(PART) == Part.BOTTOM)
            {
                worldIn.setBlock(belowPos, Blocks.AIR.defaultBlockState(), 35);
                worldIn.levelEvent(player, 2001, belowPos, Block.getId(belowState));
            }
        }
    }

    protected final int maxSingleAge;

    protected DoubleCropBlock(Properties properties, int maxSingleAge, int maxAge, Supplier<? extends Block> dead, Supplier<? extends Item> seeds, FarmlandTileEntity.NutrientType primaryNutrient)
    {
        super(properties, maxAge, dead, seeds, primaryNutrient);

        this.maxSingleAge = maxSingleAge;
        registerDefaultState(defaultBlockState().setValue(PART, Part.BOTTOM));
    }

    /**
     * Copied from {@link net.minecraft.block.DoublePlantBlock#playerWillDestroy(World, BlockPos, BlockState, PlayerEntity)}
     */
    @Override
    public void playerWillDestroy(World worldIn, BlockPos pos, BlockState state, PlayerEntity player)
    {
        if (!worldIn.isClientSide)
        {
            if (player.isCreative())
            {
                preventCreativeDropFromBottomPart(worldIn, pos, state, player);
            }
            else
            {
                dropResources(state, worldIn, pos, null, player, player.getMainHandItem());
            }
        }
        super.playerWillDestroy(worldIn, pos, state, player);
    }

    /**
     * Copied from {@link net.minecraft.block.DoublePlantBlock#playerDestroy(World, PlayerEntity, BlockPos, BlockState, TileEntity, ItemStack)}
     */
    @Override
    public void playerDestroy(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack)
    {
        super.playerDestroy(worldIn, player, pos, Blocks.AIR.defaultBlockState(), te, stack);
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader worldIn, BlockPos pos)
    {
        final Part part = state.getValue(PART);
        final BlockState belowState = worldIn.getBlockState(pos.below());
        if (part == Part.BOTTOM)
        {
            if (state.getValue(WILD))
            {
                return TFCTags.Blocks.WILD_CROP_GROWS_ON.contains(belowState.getBlock());
            }
            return belowState.getBlock() instanceof IFarmlandBlock;
        }
        else
        {
            return belowState.is(this) && belowState.getValue(PART) == Part.BOTTOM;
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        return !state.getValue(WILD) && state.getValue(PART) == Part.BOTTOM; // Bottom block is controller for the rest of the crop
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(PART));
    }

    @Override
    public void growthTick(BlockState state, World worldIn, BlockPos pos, Random rand)
    {
        // Only the bottom part should tick
        if (state.getValue(getAgeProperty()) < getMaxAge() && state.getValue(PART) == Part.BOTTOM)
        {
            // Non-wild crops must be growing on compatible farmland. Update the state of the below farmland first before doing any crop updates.
            final BlockPos belowPos = pos.below();
            final BlockState belowState = worldIn.getBlockState(belowPos);
            final CropTileEntity crop = Helpers.getTileEntityOrThrow(worldIn, pos, CropTileEntity.class);
            final IFarmlandBlock farmland = (IFarmlandBlock) belowState.getBlock();

            farmland.update(worldIn, belowPos, belowState);

            // Update the crop based on the last time it was ticked
            // todo: other checks here (temperature, mostly)
            if (farmland.getMoisture(worldIn, belowPos, belowState) == TFCFarmlandBlock.FLOWING_MOISTURE)
            {
                // Valid moisture
                long tickDelta = Calendars.SERVER.getTicks() - crop.getLastTick();
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
                worldIn.setBlock(pos, state.setValue(getAgeProperty(), age), 3);
                if (age > maxSingleAge)
                {
                    worldIn.setBlock(pos.above(), state.setValue(getAgeProperty(), age).setValue(PART, Part.TOP), 3);
                }
            }
        }
    }

    public enum Part implements IStringSerializable
    {
        BOTTOM, TOP;

        private final String serializedName;

        Part()
        {
            this.serializedName = name().toLowerCase();
        }

        @Override
        public String getSerializedName()
        {
            return serializedName;
        }
    }
}
