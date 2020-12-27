package net.dries007.tfc.common.blocks.crop;

import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ICropBlock
{
    /**
     * Triggers a growth tick on this crop
     * Note: this does not force the crop to grow, only that it should check itself for updates.
     */
    void growthTick(BlockState state, World worldIn, BlockPos pos, Random rand);
}
