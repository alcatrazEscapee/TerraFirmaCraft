package net.dries007.tfc.common.blocks.crop;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;

import net.dries007.tfc.common.blocks.TFCBlockStateProperties;

public class DoubleDeadCropBlock extends DeadCropBlock
{
    public static final EnumProperty<DoubleCropBlock.Part> PART = TFCBlockStateProperties.DOUBLE_CROP_PART;

    public DoubleDeadCropBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(PART));
    }
}
