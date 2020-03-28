/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.river;

import javax.annotation.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import net.dries007.tfc.world.TFCChunkGenerator;

public class RiverGenerator extends AbstractRiverGenerator
{
    public RiverGenerator(long seed)
    {
        super(seed);
    }

    @Nullable
    @Override
    protected BlockPos findValidDrainPos(ChunkPos pos)
    {
        return new BlockPos(pos.getMinBlockX() + random.nextInt(16), getSeaLevel(), pos.getMinBlockZ() + random.nextInt(16));
    }

    @Override
    protected int getSeaLevel()
    {
        return TFCChunkGenerator.SEA_LEVEL;
    }

    @Override
    protected boolean isValidPiece(RiverPiece piece)
    {
        return true;
    }
}
