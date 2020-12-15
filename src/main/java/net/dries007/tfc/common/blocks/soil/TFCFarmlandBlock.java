package net.dries007.tfc.common.blocks.soil;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.tileentity.FarmlandTileEntity;
import net.dries007.tfc.util.Helpers;

public class TFCFarmlandBlock extends FarmlandBlock implements ISoilBlock, IFarmlandBlock
{
    public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE; // We use this property as the superclass has the same property, but we use it differently

    public static final int SOURCE_MOISTURE = 2;
    public static final int FLOWING_MOISTURE = 1;
    public static final int NO_MOISTURE = 0;

    private static final ITextComponent FARMLAND_TOOLTIP = new TranslationTextComponent("tfc.tooltip.farmland");
    private static final ITextComponent[] HYDRATION_TOOLTIPS = {
        new TranslationTextComponent("tfc.tooltip.farmland_hydration_dry"),
        new TranslationTextComponent("tfc.tooltip.farmland_hydration_moist"),
        new TranslationTextComponent("tfc.tooltip.farmland_hydration_soaked")
    };

    public static void turnToDirt(BlockState state, World worldIn, BlockPos pos)
    {
        worldIn.setBlockAndUpdate(pos, pushEntitiesUp(state, ((TFCFarmlandBlock) state.getBlock()).getDirt(), worldIn, pos));
    }

    private final Supplier<? extends Block> dirt;
    private final float nutrientRetention;

    public TFCFarmlandBlock(Properties properties, SoilBlockType.Variant variant)
    {
        this(properties, TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(variant), variant.getDrainage());
    }

    public TFCFarmlandBlock(Properties properties, Supplier<? extends Block> dirt, float nutrientRetention)
    {
        super(properties);

        this.dirt = dirt;
        this.nutrientRetention = nutrientRetention;
    }

    @Override
    public void update(IWorld world, BlockPos pos, BlockState state)
    {
        updateMoisture(world, pos, state);
    }

    public float getNutrientRetention()
    {
        return nutrientRetention;
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        final BlockState defaultState = defaultBlockState();
        return defaultState.canSurvive(context.getLevel(), context.getClickedPos()) ? defaultState : getDirt();
    }

    @Override
    public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand)
    {
        if (!state.canSurvive(worldIn, pos))
        {
            turnToDirt(state, worldIn, pos);
        }
        else
        {
            updateMoisture(worldIn, pos, state);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random)
    {
        tick(state, worldIn, pos, random);
    }

    @Override
    public void fallOn(World worldIn, BlockPos pos, Entity entityIn, float fallDistance)
    {
        // No-op
    }

    @Override
    public BlockState getDirt()
    {
        return dirt.get().defaultBlockState();
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {
        return new FarmlandTileEntity();
    }

    @Override
    public void addHoeOverlayInfo(IWorld world, BlockPos pos, BlockState state, List<ITextComponent> lines)
    {
        // Tooltip for nutrients, and hydration
        final int hydration = state.getValue(MOISTURE);
        final FarmlandTileEntity farmland = Helpers.getTileEntityOrThrow(world, pos, FarmlandTileEntity.class);

        lines.add(FARMLAND_TOOLTIP);
        lines.add(HYDRATION_TOOLTIPS[hydration]);
        lines.add(new TranslationTextComponent("tfc.tooltip.farmland_nutrients", String.format("%2.0f", 100 * farmland.getNitrogen()), String.format("%2.0f", 100 * farmland.getPhosphorous()), String.format("%2.0f", 100 * farmland.getPotassium())));
    }

    private void updateMoisture(IWorld worldIn, BlockPos pos, BlockState state)
    {
        if (worldIn.isAreaLoaded(pos, 8))
        {
            final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
            int moisture = NO_MOISTURE;
            for (int x = -4; x <= 4 && moisture < SOURCE_MOISTURE; x++)
            {
                for (int z = -4; z <= 4 && moisture < SOURCE_MOISTURE; z++)
                {
                    for (int y = 0; y <= 1; y++)
                    {
                        mutablePos.setWithOffset(pos, x, y, z);

                        final FluidState fluid = worldIn.getBlockState(mutablePos).getFluidState();
                        if (fluid.is(TFCTags.Fluids.FRESH_WATER))
                        {
                            if (fluid.isSource())
                            {
                                moisture = SOURCE_MOISTURE;
                                break;
                            }
                            else
                            {
                                moisture = FLOWING_MOISTURE;
                            }
                        }
                    }
                }
            }

            if (state.getValue(MOISTURE) != moisture)
            {
                worldIn.setBlock(pos, state.setValue(MOISTURE, moisture), 3);
            }
        }
    }
}
