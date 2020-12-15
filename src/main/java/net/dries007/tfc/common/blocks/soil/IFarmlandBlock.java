package net.dries007.tfc.common.blocks.soil;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

import net.dries007.tfc.client.IHoeOverlayBlock;

public interface IFarmlandBlock extends IHoeOverlayBlock
{
    void update(IWorld world, BlockPos pos, BlockState state);

    /**
     * Get the moisture of the farmland at the current position.
     * 0 = No Moisture
     * 1 = Normal
     * 2 = Saturated (No Growth)
     */
    default int getMoisture(IWorld world, BlockPos pos, BlockState state)
    {
        return state.getValue(TFCFarmlandBlock.MOISTURE);
    }
}
