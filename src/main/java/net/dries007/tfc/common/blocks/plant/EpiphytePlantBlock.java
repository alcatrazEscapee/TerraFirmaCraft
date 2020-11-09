/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.common.blocks.plant;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public abstract class EpiphytePlantBlock extends PlantBlock
{
    protected static final DirectionProperty FACING = BlockStateProperties.FACING;
    protected static final VoxelShape PLANT_UP_SHAPE = box(4.0, 0.0, 4.0, 12.0, 12.0, 12.0);
    protected static final VoxelShape PLANT_DOWN_SHAPE = box(4.0, 4.0, 4.0, 12.0, 16.0, 12.0);
    protected static final VoxelShape PLANT_NORTH_SHAPE = box(0.0, 0.0, 4.0, 16.0, 16.0, 16.0);
    protected static final VoxelShape PLANT_SOUTH_SHAPE = box(0.0, 0.0, 0.0, 16.0, 16.0, 12.0);
    protected static final VoxelShape PLANT_WEST_SHAPE = box(4.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    protected static final VoxelShape PLANT_EAST_SHAPE = box(0.0, 0.0, 0.0, 12.0, 16.0, 16.0);

    protected static final VoxelShape[] SHAPES = Util.make(new VoxelShape[6], shape -> {
        shape[Direction.UP.ordinal()] = PLANT_UP_SHAPE;
        shape[Direction.DOWN.ordinal()] = PLANT_DOWN_SHAPE;
        shape[Direction.NORTH.ordinal()] = PLANT_NORTH_SHAPE;
        shape[Direction.SOUTH.ordinal()] = PLANT_SOUTH_SHAPE;
        shape[Direction.WEST.ordinal()] = PLANT_WEST_SHAPE;
        shape[Direction.EAST.ordinal()] = PLANT_EAST_SHAPE;
    });


    public EpiphytePlantBlock(Properties properties)
    {
        // Mark for post process so #updateShape is called after worldgen
        super(properties.hasPostProcess((state, reader, pos) -> true));
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos)
    {
        if (!worldIn.isEmptyBlock(currentPos.below()))
        {
            return Blocks.AIR.defaultBlockState();
        }
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            BlockState attach = worldIn.getBlockState(currentPos.relative(direction.getOpposite()));
            if (attach.getMaterial() == Material.WOOD && attach.getBlock() instanceof RotatedPillarBlock && canSupportCenter(worldIn, currentPos.relative(direction.getOpposite()), direction))
            {
                return stateIn.setValue(FACING, direction);
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader world, BlockPos pos)
    {
        Direction direction = state.getValue(FACING);
        BlockState attached = world.getBlockState(pos.relative(direction.getOpposite()));
        return world.isEmptyBlock(pos.below()) &&
            attached.getBlock() instanceof RotatedPillarBlock
            && attached.getMaterial() == Material.WOOD
            && canSupportCenter(world, pos.relative(direction.getOpposite()), direction);
    }

    @Override
    public void onPlace(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
    {
        super.onPlace(state, worldIn, pos, oldState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
    {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
