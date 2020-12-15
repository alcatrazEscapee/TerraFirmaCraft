package net.dries007.tfc.common.blocks.crop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;

import net.dries007.tfc.common.blocks.TFCBlockStateProperties;

public class ClimbingDeadCropBlock extends DoubleDeadCropBlock
{
    public static final BooleanProperty STICK = TFCBlockStateProperties.STICK;

    public ClimbingDeadCropBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(STICK));
    }
}
