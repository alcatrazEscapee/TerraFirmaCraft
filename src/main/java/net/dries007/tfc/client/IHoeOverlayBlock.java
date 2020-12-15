package net.dries007.tfc.client;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IWorld;

public interface IHoeOverlayBlock
{
    void addHoeOverlayInfo(IWorld world, BlockPos pos, BlockState state, List<ITextComponent> lines);
}
