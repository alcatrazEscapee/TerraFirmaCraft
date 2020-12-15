package net.dries007.tfc.common.blocks.crop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BushBlock;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;

import net.dries007.tfc.common.blocks.TFCBlockStateProperties;

public class DeadCropBlock extends BushBlock
{
    public static final BooleanProperty MATURE = TFCBlockStateProperties.MATURE;

    public DeadCropBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(MATURE);
    }
}
